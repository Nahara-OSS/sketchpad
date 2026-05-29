package io.github.naharaoss.skpd.brush.ui

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.StylusInput
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.utils.GLBlitProgram
import io.github.naharaoss.skpd.utils.GLFramebuffer
import io.github.naharaoss.skpd.utils.GLTexture2D
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class ScratchpadView(context: Context) : GLSurfaceView(context) {
    private val renderer = object : Renderer {
        private var width = 1
        private var height = 1
        private val rect get() = Rect(
            offset = Offset.Zero,
            size = Size(width.toFloat(), height.toFloat())
        )
        private lateinit var brushRenderer: BrushType.Renderer<BrushType.Preset>
        private lateinit var blitProgram: GLBlitProgram
        private lateinit var scratchpadTexture: GLTexture2D
        private lateinit var scratchpadFramebuffer: GLFramebuffer

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            brushRenderer = StampBrush.createTypeErasedRenderer()
            brushRenderer.usePreset(brushRenderer.type.defaultPreset)
            brushRenderer.useColor(Color.Black)
            blitProgram = GLBlitProgram(false)

            scratchpadTexture = GLTexture2D()
            scratchpadTexture.bind {
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1024, 1024, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
                minFilter = GLTexture2D.Filter.Linear
                magFilter = GLTexture2D.Filter.Nearest
                wrapS = GLTexture2D.WrapMode.Clamp
                wrapT = GLTexture2D.WrapMode.Clamp
            }

            scratchpadFramebuffer = GLFramebuffer()
            scratchpadFramebuffer.bind {
                attach(GLFramebuffer.Attachment.Color(0), scratchpadTexture)
                ensureCompleted()
                setViewport(0, 0, 1024, 1024)
                setClearColor(Color.Transparent)
                clear(GLFramebuffer.ClearType.Color)
            }
        }

        fun usePreset(preset: BrushType.Preset) {
            if (brushRenderer.type != preset.type) {
                brushRenderer.close()
                brushRenderer = preset.type.createTypeErasedRenderer()
            }

            brushRenderer.usePreset(preset)
            brushRenderer.useColor(Color.Black)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            this.width = width
            this.height = height

            scratchpadTexture.bind {
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
            }

            scratchpadFramebuffer.bind {
                setViewport(0, 0, width, height)
                setClearColor(Color.Transparent)
                clear(GLFramebuffer.ClearType.Color)
            }
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)

            GLFramebuffer.Default.bind {
                setViewport(0, 0, width, height)
                setClearColor(Color.White)
                clear(GLFramebuffer.ClearType.Color)
                blitProgram.blit(scratchpadTexture)
            }

            brushRenderer.renderTile(
                tileKey = Unit,
                tileRect = rect,
                framebuffer = GLFramebuffer.Default,
                transform = Matrix()
            )

            GLES30.glDisable(GLES30.GL_BLEND)
        }

        fun beginStroke() = brushRenderer.beginStroke()
        fun consumeInput(input: StylusInput) = brushRenderer.consumeInput(input)
        fun consumeTile() = brushRenderer.consumeTile(Unit, rect)
        fun cancelStroke() = brushRenderer.endStroke()
        fun endStroke() {
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            GLES30.glBlendEquation(GLES30.GL_FUNC_ADD)

            scratchpadFramebuffer.bind {
                setViewport(0, 0, width, height)

                brushRenderer.renderTile(
                    tileKey = Unit,
                    tileRect = rect,
                    framebuffer = scratchpadFramebuffer,
                    transform = Matrix()
                )
            }

            brushRenderer.endStroke()
            GLES30.glDisable(GLES30.GL_BLEND)
        }
    }

    private var lastInput: StylusInput? = null
    private var strokeJitter = 0f
    private var _preset: BrushType.Preset = StampBrush.defaultPreset
    var preset
        get() = _preset
        set(value) {
            if (_preset == value) return
            _preset = value
            queueEvent { renderer.usePreset(value) }
        }

    var enableScratchpad = true

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)
        if (!enableScratchpad) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                requestUnbufferedDispatch(event)
                strokeJitter = Random.nextFloat()
                val input = StylusInput.fromMotionEvent(null, event, strokeJitter)
                lastInput = input

                queueEvent {
                    renderer.beginStroke()
                    renderer.consumeInput(input)
                    renderer.consumeTile()
                }

                requestRender()
            }

            MotionEvent.ACTION_MOVE -> {
                val input = StylusInput.fromMotionEvent(lastInput, event, strokeJitter)
                lastInput = input

                queueEvent {
                    renderer.consumeInput(input)
                    renderer.consumeTile()
                }

                requestRender()
            }

            MotionEvent.ACTION_UP -> {
                val input = StylusInput.fromMotionEvent(lastInput, event, strokeJitter)
                lastInput = null

                queueEvent {
                    renderer.consumeInput(input)
                    renderer.consumeTile()
                    renderer.endStroke()
                }

                requestRender()
            }

            MotionEvent.ACTION_CANCEL -> {
                lastInput = null

                queueEvent {
                    renderer.cancelStroke()
                }

                requestRender()
            }
        }

        return true
    }
}