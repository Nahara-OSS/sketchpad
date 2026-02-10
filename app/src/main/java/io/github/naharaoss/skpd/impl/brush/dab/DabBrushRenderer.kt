package io.github.naharaoss.skpd.impl.brush.dab

import android.opengl.GLES30.*
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.engine.brush.BrushRenderer
import io.github.naharaoss.skpd.engine.brush.StylusInput
import io.github.naharaoss.skpd.engine.brush.lerp
import io.github.naharaoss.skpd.gl.GLES20Extra
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class DabBrushRenderer : BrushRenderer<DabBrush> {
    private val strokeLayers = mutableMapOf<Any, StrokeLayer>()
    private var brush: DabBrush? = null
    private var dabs: List<StrokeDab> = emptyList()
    private var prevInput: StylusInput? = null
    private val dabTexture = GLES20Extra.glGenTextures().also {
        glBindTexture(GL_TEXTURE_2D, it)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        Log.d("DabBrushRenderer", "Generated dab texture")
    }
    private val dabVertexShader = glCreateShader(GL_VERTEX_SHADER).also {
        glShaderSource(it, """
            #version 300 es
            precision mediump float;
            
            layout (location = 0) uniform mat4 uWorldToView;
            
            layout (location = 0) in vec2 aVertexPosition;
            layout (location = 1) in vec2 aTextureCoords;
            layout (location = 2) in vec2 aDabWorldPosition;
            layout (location = 3) in float aDabSize;
            layout (location = 4) in float aDabOpacity;
            layout (location = 5) in float aDabFlow;
            
            out float vDabOpacity;
            out float vDabFlow;
            out vec2 vTextureCoords;
            
            void main() {
                gl_Position = uWorldToView * vec4(aDabWorldPosition + aVertexPosition * aDabSize, 0.0, 1.0);
                vDabOpacity = aDabOpacity;
                vDabFlow = aDabFlow;
                vTextureCoords = aTextureCoords;
            }
        """.trimIndent())
        glCompileShader(it)
        Log.d("DabBrushRenderer", "Compile log for dab vertex shader: ${glGetShaderInfoLog(it)}")
    }
    private val dabFragmentShader = glCreateShader(GL_FRAGMENT_SHADER).also {
        glShaderSource(it, """
            #version 300 es
            precision mediump float;
            
            layout (location = 1) uniform sampler2D uTexture;
            
            in float vDabOpacity;
            in float vDabFlow;
            in vec2 vTextureCoords;
            
            layout (location = 0) out vec4 fragColor;
            
            void main() {
                vec4 dabColor = texture(uTexture, vTextureCoords);
                fragColor = dabColor * vDabFlow;
                gl_FragDepth = dabColor.a * vDabOpacity;
            }
        """.trimIndent())
        glCompileShader(it)
        Log.d("DabBrushRenderer", "Compile log for dab fragment shader: ${glGetShaderInfoLog(it)}")
    }
    private val dabProgram = glCreateProgram().also {
        glAttachShader(it, dabVertexShader)
        glAttachShader(it, dabFragmentShader)
        glLinkProgram(it)
        Log.d("DabBrushRenderer", "Link log for dab program: ${glGetProgramInfoLog(it)}")
    }
    private val dabVertexBuffer = GLES20Extra.glGenBuffers().also {
        val data = ByteBuffer.allocateDirect(16 * 4).apply {
            order(ByteOrder.nativeOrder())
            putFloat(-0.5f).putFloat(-0.5f).putFloat(0f).putFloat(0f)
            putFloat(+0.5f).putFloat(-0.5f).putFloat(1f).putFloat(0f)
            putFloat(-0.5f).putFloat(+0.5f).putFloat(0f).putFloat(1f)
            putFloat(+0.5f).putFloat(+0.5f).putFloat(1f).putFloat(1f)
            flip()
        }
        glBindBuffer(GL_ARRAY_BUFFER, it)
        glBufferData(GL_ARRAY_BUFFER, data.capacity(), data, GL_STATIC_DRAW)
    }
    private val dabInstanceBuffer = object : AutoCloseable {
        private val SIZE_PER_INSTANCE = 4 * 5
        private var capacity = 128
        private var byteBuffer = ByteBuffer
            .allocateDirect(capacity * SIZE_PER_INSTANCE)
            .order(ByteOrder.nativeOrder())
        val id = GLES20Extra.glGenBuffers()

        fun fill(dabs: List<StrokeDab>) {
            if (dabs.size > capacity) {
                while (dabs.size > capacity) capacity *= 2
                byteBuffer = ByteBuffer
                    .allocateDirect(capacity * SIZE_PER_INSTANCE)
                    .order(ByteOrder.nativeOrder())
            }

            byteBuffer.clear()

            for (dab in dabs) {
                byteBuffer.putFloat(dab.x)
                byteBuffer.putFloat(dab.y)
                byteBuffer.putFloat(dab.size)
                byteBuffer.putFloat(dab.opacity)
                byteBuffer.putFloat(dab.flow)
            }

            byteBuffer.flip()
            glBindBuffer(GL_ARRAY_BUFFER, id)
            glBufferData(GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, GL_DYNAMIC_DRAW)
        }

        override fun close() {
            GLES20Extra.glDeleteBuffer(id)
        }
    }
    private val blitVertexShader = glCreateShader(GL_VERTEX_SHADER).also {
        glShaderSource(it, """
            #version 300 es
            precision mediump float;
            
            layout (location = 0) in vec4 aPosition;
            layout (location = 1) in vec2 aTextureCoords;
            
            out vec2 vTextureCoords;
            
            void main() {
                gl_Position = aPosition;
                vTextureCoords = aTextureCoords;
            }
        """.trimIndent())
        glCompileShader(it)
        Log.d("DabBrushRenderer", "Compile log for blit vertex shader: ${glGetShaderInfoLog(it)}")
    }
    private val blitFragmentShader = glCreateShader(GL_FRAGMENT_SHADER).also {
        glShaderSource(it, """
            #version 300 es
            precision mediump float;
            
            layout (location = 0) uniform sampler2D uTexture;
            layout (location = 1) uniform sampler2D uDepthTexture;
            
            in vec2 vTextureCoords;
            
            layout (location = 0) out vec4 fragColor;
            
            void main() {
                vec4 color = texture(uTexture, vTextureCoords);
                float depth = texture(uDepthTexture, vTextureCoords).r;
                fragColor = color * depth;
            }
        """.trimIndent())
        glCompileShader(it)
        Log.d("DabBrushRenderer", "Compile log for blit fragment shader: ${glGetShaderInfoLog(it)}")
    }
    private val blitProgram = glCreateProgram().also {
        glAttachShader(it, blitVertexShader)
        glAttachShader(it, blitFragmentShader)
        glLinkProgram(it)
        Log.d("DabBrushRenderer", "Link log for blit program: ${glGetProgramInfoLog(it)}")
    }
    private val blitVertexBuffer = GLES20Extra.glGenBuffers().also {
        val data = ByteBuffer.allocateDirect(16 * 4).apply {
            order(ByteOrder.nativeOrder())
            putFloat(-1f).putFloat(-1f).putFloat(0f).putFloat(0f)
            putFloat(+1f).putFloat(-1f).putFloat(1f).putFloat(0f)
            putFloat(-1f).putFloat(+1f).putFloat(0f).putFloat(1f)
            putFloat(+1f).putFloat(+1f).putFloat(1f).putFloat(1f)
            flip()
        }
        glBindBuffer(GL_ARRAY_BUFFER, it)
        glBufferData(GL_ARRAY_BUFFER, data.capacity(), data, GL_STATIC_DRAW)
    }

    private data class StrokeDab(
        val x: Float,
        val y: Float,
        val size: Float,
        val opacity: Float,
        val flow: Float
    ) {
        constructor(brush: DabBrush, prev: StylusInput?, next: StylusInput) : this(
            x = next.x + brush.offsetX.apply(prev, next) + brush.scatterX.apply(prev, next) * (Random.nextFloat() * 2f - 1f),
            y = next.y + brush.offsetY.apply(prev, next) + brush.scatterY.apply(prev, next) * (Random.nextFloat() * 2f - 1f),
            size = max(brush.size.apply(prev, next), 0f),
            opacity = min(max(brush.opacity.apply(prev, next), 0f), 1f),
            flow = min(max(brush.flow.apply(prev, next), 0f), 1f),
        )

        val bounds get() = Rect(Offset(x, y), size / 2f)
    }

    private class StrokeLayer(val width: Int, val height: Int) : AutoCloseable {
        var dirty = true

        val colorTexture = GLES20Extra.glGenTextures().also {
            glBindTexture(GL_TEXTURE_2D, it)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
        }

        /**
         * Depth texture will be used as alpha mask when drawing stroke layer on top of canvas. The
         * depth function is [GL_GREATER] and the `gl_FragDepth` is the alpha value of shape bitmap
         * scaled by opacity.
         */
        val depthTexture = GLES20Extra.glGenTextures().also {
            glBindTexture(GL_TEXTURE_2D, it)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_NONE)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT16, width, height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, null)
        }

        val framebuffer = GLES20Extra.glGenFramebuffers().also {
            glBindFramebuffer(GL_FRAMEBUFFER, it)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0)
            glClearColor(0f, 0f, 0f, 0f)
            glClearDepthf(0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        }

        override fun close() {
            GLES20Extra.glDeleteFramebuffers(framebuffer)
            GLES20Extra.glDeleteTextures(colorTexture)
            GLES20Extra.glDeleteTextures(depthTexture)
        }
    }

    override fun begin(brush: DabBrush) {
        for (strokeLayer in strokeLayers.values) strokeLayer.close()
        strokeLayers.clear()
        this.brush = brush
        dabs = emptyList()
        prevInput = null

        glBindTexture(GL_TEXTURE_2D, dabTexture)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            brush.shape.bitmapWidth,
            brush.shape.bitmapHeight,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            ByteBuffer
                .allocateDirect(brush.shape.bitmapWidth * brush.shape.bitmapHeight * 4)
                .order(ByteOrder.nativeOrder())
                .also { brush.shape.writeBitmap(it) }
                .also { it.flip() }
        )
    }

    override fun consume(inputs: List<StylusInput>): Rect? {
        val brush = this.brush ?: return null
        val dabs = mutableListOf<StrokeDab>()
        var bounds: Rect? = null

        fun Rect?.expand(another: Rect) = if (this == null) another else Rect(
            left = min(left, another.left),
            right = max(right, another.right),
            top = min(top, another.top),
            bottom = max(bottom, another.bottom)
        )

        for (i in 0..<inputs.size) {
            val prev = if (i == 0) prevInput else inputs[i - 1]
            val next = inputs[i]

            if (prev == null) {
                val dab = StrokeDab(brush, null, next)
                dabs.add(dab)
                bounds = bounds.expand(dab.bounds)
                prevInput = next
                continue
            }

            val distance = prev distanceTo next
            var d = max(brush.spacing.apply(prev, next), 0.1f)

            while (d <= distance) {
                val input = lerp(prev, next, d / distance)
                val dab = StrokeDab(brush, prevInput!!, input)
                dabs.add(dab)
                bounds = bounds.expand(dab.bounds)
                prevInput = input
                d += max(brush.spacing.apply(prev, next), 0.1f)
            }
        }

        this.dabs = dabs
        dabInstanceBuffer.fill(dabs)
        for ((_, strokeLayer) in strokeLayers) strokeLayer.dirty = true // TODO: Only mark tiles that are intersecting bounds
        return bounds
    }

    override fun processTile(
        worldToView: Matrix,
        key: Any,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        val strokeLayer = strokeLayers.getOrPut(key) { StrokeLayer(width, height) }

        glBindFramebuffer(GL_FRAMEBUFFER, strokeLayer.framebuffer)
        glViewport(0, 0, width, height)
        glUseProgram(dabProgram)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, dabTexture)
        glUniform1i(1, 0)
        glUniformMatrix4fv(0, 1, false, worldToView.values, 0)

        glEnableVertexAttribArray(0)
        glVertexAttribDivisor(0, 0)
        glBindBuffer(GL_ARRAY_BUFFER, dabVertexBuffer)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0)

        glEnableVertexAttribArray(1)
        glVertexAttribDivisor(1, 0)
        glBindBuffer(GL_ARRAY_BUFFER, dabVertexBuffer)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8)

        glEnableVertexAttribArray(2)
        glVertexAttribDivisor(2, 1)
        glBindBuffer(GL_ARRAY_BUFFER, dabInstanceBuffer.id)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 20, 0)

        glEnableVertexAttribArray(3)
        glVertexAttribDivisor(3, 1)
        glBindBuffer(GL_ARRAY_BUFFER, dabInstanceBuffer.id)
        glVertexAttribPointer(3, 1, GL_FLOAT, false, 20, 8)

        glEnableVertexAttribArray(4)
        glVertexAttribDivisor(4, 1)
        glBindBuffer(GL_ARRAY_BUFFER, dabInstanceBuffer.id)
        glVertexAttribPointer(4, 1, GL_FLOAT, false, 20, 12)

        glEnableVertexAttribArray(5)
        glVertexAttribDivisor(5, 1)
        glBindBuffer(GL_ARRAY_BUFFER, dabInstanceBuffer.id)
        glVertexAttribPointer(5, 1, GL_FLOAT, false, 20, 16)

        // Stroke opacity/alpha mask
        glColorMask(false, false, false, false)
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_GREATER)
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, dabs.size)
        glDisable(GL_DEPTH_TEST)

        // Stroke color
        // Blending mode for each individual dab is always source over with premultiplied alpha
        glColorMask(true, true, true, true)
        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        glBlendEquation(GL_FUNC_ADD)
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, dabs.size)
        glDisable(GL_BLEND)

        glVertexAttribDivisor(2, 0)
        glVertexAttribDivisor(3, 0)
        glVertexAttribDivisor(4, 0)
        glVertexAttribDivisor(5, 0)
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(2)
        glDisableVertexAttribArray(3)
        glDisableVertexAttribArray(4)
        glDisableVertexAttribArray(5)
    }

    override fun renderToCanvas(
        canvasFb: Int,
        worldToView: Matrix,
        key: Any,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        val brush = this.brush ?: return
        val strokeLayer = strokeLayers.getOrPut(key) { StrokeLayer(width, height) }

        glBindFramebuffer(GL_FRAMEBUFFER, canvasFb)
        glViewport(0, 0, width, height)
        glUseProgram(blitProgram)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, strokeLayer.colorTexture)
        glUniform1i(0, 0)

        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, strokeLayer.depthTexture)
        glUniform1i(1, 1)

        glEnableVertexAttribArray(0)
        glVertexAttribDivisor(0, 0)
        glBindBuffer(GL_ARRAY_BUFFER, blitVertexBuffer)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0)

        glEnableVertexAttribArray(1)
        glVertexAttribDivisor(1, 0)
        glBindBuffer(GL_ARRAY_BUFFER, blitVertexBuffer)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8)

        glEnable(GL_BLEND) // TODO: Configure blending mode from current brush
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        glBlendEquation(GL_FUNC_ADD)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glDisable(GL_BLEND)

        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
    }

    override fun close() {
        for (strokeLayer in strokeLayers.values) strokeLayer.close()
        strokeLayers.clear()
        glDeleteProgram(dabProgram)
        glDeleteShader(dabVertexShader)
        glDeleteShader(dabFragmentShader)
        glDeleteProgram(blitProgram)
        glDeleteShader(blitVertexShader)
        glDeleteShader(blitFragmentShader)
        GLES20Extra.glDeleteBuffer(dabVertexBuffer)
        GLES20Extra.glDeleteBuffer(blitVertexBuffer)
        dabInstanceBuffer.close()
    }
}