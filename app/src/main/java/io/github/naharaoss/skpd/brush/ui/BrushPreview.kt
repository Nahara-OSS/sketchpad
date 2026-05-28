package io.github.naharaoss.skpd.brush.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.util.Log
import android.view.TextureView
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.viewinterop.AndroidView
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.StylusInput
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.utils.GLFramebuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun BrushPreview(
    modifier: Modifier = Modifier,
    preset: BrushType.Preset,
    color: Color = Color.Unspecified
) {
    val strokeColor = if (color == Color.Unspecified) LocalContentColor.current else color

    AndroidView(
        modifier = modifier,
        factory = { BrushPreviewView(it, preset) }
    ) { view ->
        view.preset = preset
        view.color = strokeColor
    }
}

@SuppressLint("ViewConstructor")
class BrushPreviewView(context: Context, initialPreset: BrushType.Preset) : TextureView(context), TextureView.SurfaceTextureListener {
    private var renderThread: BrushPreviewRenderThread? = null
    private var _color: Color = Color.Black
    private var _preset: BrushType.Preset = initialPreset

    var color
        get() = _color
        set(value) {
            if (_color == value) return
            _color = value
            renderThread?.useColor(value)
        }

    var preset
        get() = _preset
        set(value) {
            if (_preset == value) return
            _preset = value
            renderThread?.usePreset(value)
        }

    init {
        surfaceTextureListener = this
        alpha = 254f / 255f // TODO: Do something about this trick

        // Compose did some kind of "optimization" where at 100% alpha, it would just cut a hole,
        // which ended up exposing the SurfaceView layer from the behind
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        renderThread?.quit()
        renderThread = BrushPreviewRenderThread(surface, width, height, _preset, _color).also { it.start() }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        renderThread?.quit()
        renderThread = null
        return true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderThread?.resize(width, height)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}

private class BrushPreviewRenderThread(
    private val surface: SurfaceTexture,
    private var width: Int,
    private var height: Int,
    private var preset: BrushType.Preset,
    private var color: Color
) : Thread("BrushPreviewRenderThread") {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var shouldStop = false
    private var oldPreset = preset
    private var oldColor = color

    override fun run() {
        // https://registry.khronos.org/EGL/sdk/docs/man/
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val eglVersion = intArrayOf(0, 0)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) throw Exception("Unable to get EGL display: eglGetDisplay()")
        if (!EGL14.eglInitialize(eglDisplay, eglVersion, 0, eglVersion, 1)) throw Exception("Unable to initialize EGL: eglInitialize()")

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        val configAttrs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )
        if (!EGL14.eglChooseConfig(eglDisplay, configAttrs, 0, configs, 0, 1, numConfigs, 0)) throw Exception("Unable to get EGL config: eglChooseConfig()")
        val eglConfig = configs[0] ?: throw Exception("No EGL configs found")

        val contextAttrs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL14.EGL_NONE
        )
        val eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttrs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) throw Exception("Unable to create EGL context: eglCreateContext()")

        val surfaceAttrs = intArrayOf(EGL14.EGL_NONE)
        val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttrs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) throw Exception("Unable to create EGL surface: eglSurface()")

        // Initializing
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) throw Exception("Unable to attach to OpenGL ES context")
        var brushRenderer = preset.type.createTypeErasedRenderer()
        brushRenderer.usePreset(preset)
        brushRenderer.useColor(color)
        brushRenderer.beginStroke()

        fun drawPreviewPath() {
            val preset = preset
            val quality = 64
            val inset = when (preset) {
                is StampBrush.Preset -> preset.size.base / 2f
                else -> 0f
            }

            for (i in 0..quality) {
                val progress = i.toFloat() / quality
                val sine = sin(progress * 2f * PI.toFloat())
                val time = progress * 1f
                val x = inset + (width - inset * 2f) * progress
                val y = inset + (height - inset * 2f) * (1f - (sine + 1f) / 2f)
                val velocity = 1f
                val pressure = sin(progress * PI.toFloat())
                val altitude = 90f
                val azimuth = 0f
                val rotation = 0f
                val input = StylusInput(time, x, y, velocity, pressure, altitude, azimuth, rotation)
                brushRenderer.consumeInput(input)
                brushRenderer.consumeTile(
                    tileKey = Unit,
                    tileRect = Rect(
                        topLeft = Offset.Zero,
                        bottomRight = Offset(x = width.toFloat(), y = height.toFloat())
                    )
                )
            }
        }

        drawPreviewPath()

        // Render loop
        while (true) {
            when {
                shouldStop -> {
                    break
                }
                else -> {
                    var redraw = false
                    if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) throw Exception("Unable to attach to OpenGL ES context")

                    if (brushRenderer.type != preset.type) {
                        brushRenderer.close()
                        brushRenderer = preset.type.createTypeErasedRenderer()
                    }

                    if (oldColor != color) {
                        brushRenderer.useColor(color)
                        oldColor = color
                        redraw = true
                    }

                    if (oldPreset != preset) {
                        brushRenderer.usePreset(preset)
                        oldPreset = preset
                        redraw = true
                    }

                    if (redraw) {
                        brushRenderer.endStroke()
                        brushRenderer.beginStroke()
                        drawPreviewPath()
                    }

                    brushRenderer.renderTile(
                        tileKey = Unit,
                        tileRect = Rect(
                            topLeft = Offset.Zero,
                            bottomRight = Offset(x = width.toFloat(), y = height.toFloat())
                        ),
                        framebuffer = GLFramebuffer.Default,
                        transform = Matrix()
                    )

                    if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) throw Exception("Unable to swap buffer: eglSwapBuffers()")
                }
            }

            lock.withLock { condition.await() }
        }

        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
    }

    fun usePreset(preset: BrushType.Preset) = lock.withLock {
        this.preset = preset
        condition.signal()
    }

    fun useColor(color: Color) = lock.withLock {
        this.color = color
        condition.signal()
    }

    fun resize(width: Int, height: Int) = lock.withLock {
        this.width = width
        this.height = height
        condition.signal()
    }

    fun quit() = lock.withLock {
        shouldStop = true
        condition.signal()
    }
}