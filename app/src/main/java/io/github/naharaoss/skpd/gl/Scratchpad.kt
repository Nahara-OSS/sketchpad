package io.github.naharaoss.skpd.gl

import android.opengl.GLES20.*
import android.opengl.GLES30.glVertexAttribDivisor
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.engine.brush.Brush
import io.github.naharaoss.skpd.engine.brush.BrushRenderer
import io.github.naharaoss.skpd.engine.brush.BrushRendererFactory
import io.github.naharaoss.skpd.engine.brush.StylusInput
import io.github.naharaoss.skpd.engine.brush.factoryMapOf
import io.github.naharaoss.skpd.engine.brush.factoryOf
import io.github.naharaoss.skpd.impl.brush.dab.DabBrushRenderer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass

@WorkerThread
class Scratchpad(val width: Int, val height: Int) : AutoCloseable {
    private object ScratchpadKey

    private val matrix = Matrix(
        floatArrayOf(
            2f / width, 0f, 0f, 0f,
            0f, -2f / height, 0f, 0f,
            0f, 0f, 1f, 0f,
            -1f, 1f, 0f, 1f,
        )
    )
    private var using = false
    private var _brushPreset: Brush? = null
    private var brushRenderer: BrushRenderer<out Brush>? = null
    private val canvasFramebuffer = GLES20Extra.glGenFramebuffers()
    val canvasTexture = GLES20Extra.glGenTextures()

    private val blitVertexShader = glCreateShader(GL_VERTEX_SHADER).also {
        glShaderSource(
            it, """
            #version 300 es
            precision mediump float;
            
            layout (location = 0) in vec4 aPosition;
            layout (location = 1) in vec2 aTextureCoords;
            
            out vec2 vTextureCoords;
            
            void main() {
                gl_Position = aPosition;
                vTextureCoords = aTextureCoords;
            }
        """.trimIndent()
        )
        glCompileShader(it)
        Log.d("Scratchpad", "Compile log for blit vertex shader: ${glGetShaderInfoLog(it)}")
    }
    private val blitFragmentShader = glCreateShader(GL_FRAGMENT_SHADER).also {
        glShaderSource(
            it, """
            #version 300 es
            precision mediump float;
            
            layout (location = 0) uniform sampler2D uTexture;
            
            in vec2 vTextureCoords;
            
            layout (location = 0) out vec4 fragColor;
            
            void main() {
                fragColor = texture(uTexture, vTextureCoords);
            }
        """.trimIndent()
        )
        glCompileShader(it)
        Log.d("Scratchpad", "Compile log for blit fragment shader: ${glGetShaderInfoLog(it)}")
    }
    private val blitProgram = glCreateProgram().also {
        glAttachShader(it, blitVertexShader)
        glAttachShader(it, blitFragmentShader)
        glLinkProgram(it)
        Log.d("Scratchpad", "Link log for blit program: ${glGetProgramInfoLog(it)}")
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

    init {
        glBindTexture(GL_TEXTURE_2D, canvasTexture)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            width,
            height,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            null
        )

        glBindFramebuffer(GL_FRAMEBUFFER, canvasFramebuffer)
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            canvasTexture,
            0
        )
        glViewport(0, 0, width, height)
        glClearColor(1f, 1f, 1f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
    }

    var brushPreset
        get() = _brushPreset
        set(value) {
            if (value == _brushPreset) return

            val prevBrushPreset = brushRenderer
            val clazz = if (value != null) value::class else null
            val prevClazz = if (prevBrushPreset != null) prevBrushPreset::class else null
            _brushPreset = value

            if (prevClazz == clazz) return

            brushRenderer?.close()
            brushRenderer = renderers[clazz]?.factory()
        }

    var renderers: Map<KClass<out Brush>, BrushRendererFactory> = factoryMapOf(
        factoryOf { DabBrushRenderer() }
    )

    fun beginStroke() {
        if (using) return
        brushRenderer?.uncheckedBegin(_brushPreset!!)
        using = true
    }

    fun consumeInputs(inputs: List<StylusInput>) {
        if (!using) return
        brushRenderer?.consume(inputs)
        brushRenderer?.processTile(
            matrix,
            ScratchpadKey,
            0,
            0,
            width,
            height
        )
    }

    fun endStroke() {
        if (!using) return
        brushRenderer?.renderToCanvas(canvasFramebuffer, matrix, ScratchpadKey, 0, 0, width, height)
        using = false
    }

    fun render(fb: Int) {
        glBindFramebuffer(GL_FRAMEBUFFER, fb)
        glViewport(0, 0, width, height)
        glUseProgram(blitProgram)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, canvasTexture)
        glUniform1i(0, 0)

        glEnableVertexAttribArray(0)
        glVertexAttribDivisor(0, 0)
        glBindBuffer(GL_ARRAY_BUFFER, blitVertexBuffer)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0)

        glEnableVertexAttribArray(1)
        glVertexAttribDivisor(1, 0)
        glBindBuffer(GL_ARRAY_BUFFER, blitVertexBuffer)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8)

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)

        if (using) {
            brushRenderer?.renderToCanvas(
                fb,
                matrix,
                ScratchpadKey,
                0,
                0,
                width,
                height
            )
        }
    }

    override fun close() {
        _brushPreset = null
        brushRenderer?.close()
        brushRenderer = null
        GLES20Extra.glDeleteFramebuffers(canvasFramebuffer)
        GLES20Extra.glDeleteTextures(canvasTexture)
        glDeleteProgram(blitProgram)
        glDeleteShader(blitVertexShader)
        glDeleteShader(blitFragmentShader)
        GLES20Extra.glDeleteBuffer(blitVertexBuffer)
    }
}