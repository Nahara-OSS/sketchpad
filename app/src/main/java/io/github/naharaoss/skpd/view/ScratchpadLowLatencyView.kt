package io.github.naharaoss.skpd.view

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLES30.glVertexAttribDivisor
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.compose.ui.graphics.Matrix
import androidx.graphics.lowlatency.BufferInfo
import androidx.graphics.lowlatency.GLFrontBufferedRenderer
import androidx.graphics.opengl.egl.EGLManager
import io.github.naharaoss.skpd.engine.brush.Brush
import io.github.naharaoss.skpd.engine.brush.StylusInput
import io.github.naharaoss.skpd.gl.GLES20Extra
import io.github.naharaoss.skpd.gl.Scratchpad
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.properties.Delegates

class ScratchpadLowLatencyView(context: Context) : SurfaceView(context) {
    private var _brushPreset: Brush? = null

    private val handler = object : GLFrontBufferedRenderer.Callback<Unit> {
        private var initialized = false
        private var auxFramebuffer by Delegates.notNull<Int>()
        private var auxTexture by Delegates.notNull<Int>()
        private var blitVertexShader by Delegates.notNull<Int>()
        private var blitFragmentShader by Delegates.notNull<Int>()
        private var blitProgram by Delegates.notNull<Int>()
        private var blitVertexBuffer by Delegates.notNull<Int>()
        var scratchpad: Scratchpad? = null

        override fun onDrawFrontBufferedLayer(
            eglManager: EGLManager,
            width: Int,
            height: Int,
            bufferInfo: BufferInfo,
            transform: FloatArray,
            param: Unit
        ) {
            render(bufferInfo)
        }

        override fun onDrawMultiBufferedLayer(
            eglManager: EGLManager,
            width: Int,
            height: Int,
            bufferInfo: BufferInfo,
            transform: FloatArray,
            params: Collection<Unit>
        ) {
            render(bufferInfo)
        }

        private fun render(bufferInfo: BufferInfo) {
            if (!initialized) {
                initialized = true
                initialize()
            }

            if (scratchpad?.width != bufferInfo.width || scratchpad?.height != bufferInfo.height) {
                scratchpad?.close()
                scratchpad = Scratchpad(bufferInfo.width, bufferInfo.height)
                scratchpad?.brushPreset = _brushPreset

                glBindTexture(GL_TEXTURE_2D, auxTexture)
                glTexImage2D(
                    GL_TEXTURE_2D,
                    0,
                    GL_RGBA,
                    bufferInfo.width,
                    bufferInfo.height,
                    0,
                    GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    null
                )
            }

            scratchpad?.render(auxFramebuffer)

            glBindFramebuffer(GL_FRAMEBUFFER, bufferInfo.frameBufferId)
            glViewport(0, 0, bufferInfo.width, bufferInfo.height)
            glUseProgram(blitProgram)

            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, auxTexture)
            glUniform1i(0, 0)
            glUniformMatrix4fv(1, 1, false, Matrix().apply { scale(1f, -1f, 1f) }.values, 0)

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
        }

        private fun initialize() {
            auxTexture = GLES20Extra.glGenTextures()
            glBindTexture(GL_TEXTURE_2D, auxTexture)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)

            auxFramebuffer = GLES20Extra.glGenFramebuffers()
            glBindFramebuffer(GL_FRAMEBUFFER, auxFramebuffer)
            glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                auxTexture,
                0
            )

            blitVertexShader = glCreateShader(GL_VERTEX_SHADER).also {
                glShaderSource(
                    it, """
                    #version 300 es
                    precision mediump float;
                    
                    layout (location = 1) uniform mat4 uTransform;
                    
                    layout (location = 0) in vec4 aPosition;
                    layout (location = 1) in vec2 aTextureCoords;
                    
                    out vec2 vTextureCoords;
                    
                    void main() {
                        gl_Position = uTransform * aPosition;
                        vTextureCoords = aTextureCoords;
                    }
                    """.trimIndent()
                )
                glCompileShader(it)
                Log.d("Scratchpad", "Compile log for blit vertex shader: ${glGetShaderInfoLog(it)}")
            }

            blitFragmentShader = glCreateShader(GL_FRAGMENT_SHADER).also {
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
                Log.d(
                    "Scratchpad",
                    "Compile log for blit fragment shader: ${glGetShaderInfoLog(it)}"
                )
            }

            blitProgram = glCreateProgram().also {
                glAttachShader(it, blitVertexShader)
                glAttachShader(it, blitFragmentShader)
                glLinkProgram(it)
                Log.d("Scratchpad", "Link log for blit program: ${glGetProgramInfoLog(it)}")
            }

            blitVertexBuffer = GLES20Extra.glGenBuffers().also {
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
        }
    }

    private val renderer = GLFrontBufferedRenderer(this, handler)

    var brushPreset
        get() = _brushPreset
        set(value) {
            _brushPreset = value
            renderer.execute { handler.scratchpad?.brushPreset = value }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                requestUnbufferedDispatch(event)
                val inputs = listOf(StylusInput(event))
                renderer.execute {
                    handler.scratchpad?.beginStroke()
                    handler.scratchpad?.consumeInputs(inputs)
                }
                renderer.renderFrontBufferedLayer(Unit)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val inputs = listOf(StylusInput(event))
                renderer.execute { handler.scratchpad?.consumeInputs(inputs) }
                renderer.renderFrontBufferedLayer(Unit)
                return true
            }

            MotionEvent.ACTION_UP -> {
                val inputs = listOf(StylusInput(event).copy(pressure = 0f))
                renderer.execute {
                    handler.scratchpad?.consumeInputs(inputs)
                    handler.scratchpad?.endStroke()
                }
                renderer.commit()
                return true
            }

            else -> {
                return false
            }
        }
    }
}