package io.github.naharaoss.skpd.view

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import io.github.naharaoss.skpd.engine.brush.Brush
import io.github.naharaoss.skpd.engine.brush.StylusInput
import io.github.naharaoss.skpd.gl.Scratchpad
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ScratchpadView(context: Context) : GLSurfaceView(context) {
    private var _brushPreset: Brush? = null

    private val renderer = object : Renderer {
        var scratchpad: Scratchpad? = null

        override fun onSurfaceCreated(
            gl: GL10?,
            config: EGLConfig?
        ) {
        }

        override fun onSurfaceChanged(
            gl: GL10?,
            width: Int,
            height: Int
        ) {
            scratchpad?.close()
            scratchpad = Scratchpad(width, height)
            scratchpad?.brushPreset = _brushPreset
        }

        override fun onDrawFrame(gl: GL10?) {
            scratchpad?.render(0)
        }
    }

    var brushPreset
        get() = _brushPreset
        set(value) {
            _brushPreset = value
            queueEvent { renderer.scratchpad?.brushPreset = value }
        }

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                requestUnbufferedDispatch(event)
                val inputs = listOf(StylusInput(event))
                queueEvent {
                    renderer.scratchpad?.beginStroke()
                    renderer.scratchpad?.consumeInputs(inputs)
                }
                requestRender()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val inputs = listOf(StylusInput(event))
                queueEvent { renderer.scratchpad?.consumeInputs(inputs) }
                requestRender()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val inputs = listOf(StylusInput(event).copy(pressure = 0f))
                queueEvent {
                    renderer.scratchpad?.consumeInputs(inputs)
                    renderer.scratchpad?.endStroke()
                }
                requestRender()
                return true
            }

            else -> {
                return false
            }
        }
    }
}