package io.github.naharaoss.skpd.brush.graphics

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Log
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.StylusInput
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.utils.Color
import io.github.naharaoss.skpd.utils.GLFramebuffer
import io.github.naharaoss.skpd.utils.Matrix4
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlin.math.PI
import kotlin.math.sin

class BrushPreviewRenderer : Thread("BrushPreviewRenderer"), AutoCloseable, RememberObserver {
    private val commands = Channel<Command>(capacity = Channel.UNLIMITED)
    private val instances = mutableMapOf<Any, Instance>()

    init {
        start()
    }

    private class Instance(
        surface: SurfaceTexture,
        private var width: Int,
        private var height: Int,
        private var preset: BrushType.Preset,
        private var color: Color.Rgb,
        private val eglDisplay: EGLDisplay,
        private val eglConfig: EGLConfig,
        private val eglContext: EGLContext
    ) : AutoCloseable {
        private var surface: Pair<SurfaceTexture, EGLSurface>
        private var brushRenderer: BrushType.Renderer<BrushType.Preset>? = null

        init {
            val surfaceAttrs = intArrayOf(EGL14.EGL_NONE)
            val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttrs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) throw Exception("Unable to create EGL surface: eglSurface()")
            this.surface = surface to eglSurface
        }

        fun reconfigure(surface: SurfaceTexture, width: Int, height: Int) {
            if (this.surface.first != surface) {
                EGL14.eglDestroySurface(eglDisplay, this.surface.second)
                val surfaceAttrs = intArrayOf(EGL14.EGL_NONE)
                val eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttrs, 0)
                if (eglSurface == EGL14.EGL_NO_SURFACE) throw Exception("Unable to create EGL surface: eglSurface()")
                this.surface = surface to eglSurface
            }

            this.width = width
            this.height = height
        }

        fun changeParams(preset: BrushType.Preset, color: Color.Rgb) {
            this.preset = preset
            this.color = color
        }

        fun tryRender(): Boolean {
            val preset = preset
            val success = EGL14.eglMakeCurrent(eglDisplay, surface.second, surface.second, eglContext)
            if (!success) return false

            if (brushRenderer?.type != preset.type) {
                brushRenderer?.close()
                brushRenderer = preset.type.createTypeErasedRenderer()
            }

            brushRenderer?.useColor(color)
            brushRenderer?.usePreset(preset)
            brushRenderer?.beginStroke()

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
                val strokeJitter = 0f
                val input = StylusInput(time, x, y, velocity, pressure, altitude, azimuth, rotation, strokeJitter)
                brushRenderer?.consumeInput(input)
                brushRenderer?.consumeTile(
                    tileKey = Unit,
                    tileRect = Rect(
                        topLeft = Offset.Zero,
                        bottomRight = Offset(x = width.toFloat(), y = height.toFloat())
                    )
                )
            }

            brushRenderer?.renderTile(
                tileKey = Unit,
                tileRect = Rect(
                    topLeft = Offset.Zero,
                    bottomRight = Offset(x = width.toFloat(), y = height.toFloat())
                ),
                framebuffer = GLFramebuffer.default(width, height),
                transform = Matrix4.Identity
            )

            brushRenderer?.endStroke()
            EGL14.eglSwapBuffers(eglDisplay, surface.second) // We ignore swap buffer error for now...
            return true
        }

        override fun close() {
            if (EGL14.eglMakeCurrent(eglDisplay, surface.second, surface.second, eglContext)) {
                brushRenderer?.close()
                brushRenderer = null
            }

            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, surface.second)
            surface.first.release()
        }
    }

    override fun run() {
        // https://registry.khronos.org/EGL/sdk/docs/man/
        Log.d("BrushPreviewRenderer", "Begin rendering thread")
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

        val contextAttrs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        val eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttrs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) throw Exception("Unable to create EGL context: eglCreateContext()")

        runBlocking {
            Log.d("BrushPreviewRenderer", "Ready to receive commands")

            for (command in commands) {
                when (command) {
                    is Command.Init -> {
                        val (sender, surface, width, height, preset, color) = command
                        val instance = Instance(surface, width, height, preset, color, eglDisplay, eglConfig, eglContext)
                        instances[sender] = instance
                    }

                    is Command.UpdateSurface -> {
                        val (sender, surface, width, height) = command
                        instances[sender]?.reconfigure(surface, width, height)
                    }

                    is Command.UpdateParams -> {
                        val (sender, preset, color) = command
                        instances[sender]?.changeParams(preset, color)
                    }

                    is Command.Close -> {
                        instances.remove(command.sender)?.close()
                    }

                    else -> {}
                }

                instances[command.sender]?.let {
                    if (!it.tryRender()) commands.trySend(Command.Render(command.sender))
                }
            }
        }

        instances.forEach { (_, instance) -> instance.close() }
        instances.clear()
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
        Log.d("BrushPreviewRenderer", "End of rendering thread")
    }

    override fun close() {
        commands.close()
    }

    private sealed interface Command {
        val sender: Any

        data class Init(
            override val sender: Any,
            val surface: SurfaceTexture,
            val width: Int,
            val height: Int,
            val preset: BrushType.Preset,
            val color: Color.Rgb
        ) : Command

        data class UpdateSurface(
            override val sender: Any,
            val surface: SurfaceTexture,
            val width: Int,
            val height: Int
        ) : Command

        data class UpdateParams(
            override val sender: Any,
            val preset: BrushType.Preset,
            val color: Color.Rgb
        ) : Command

        data class Render(
            override val sender: Any
        ) : Command

        data class Close(
            override val sender: Any,
            val surface: SurfaceTexture
        ) : Command
    }

    override fun onRemembered() {}
    override fun onForgotten() = close()
    override fun onAbandoned() = close()

    fun createSurfaceTextureListener(preset: BrushType.Preset, color: Color.Rgb) = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            commands.trySend(Command.Init(this, surface, width, height, preset, color))
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            commands.trySend(Command.UpdateSurface(this, surface, width, height))
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            commands.trySend(Command.Close(this, surface))
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // TODO
        }

        override fun changeParams(preset: BrushType.Preset, color: Color.Rgb) {
            commands.trySend(Command.UpdateParams(this, preset, color))
        }
    }

    interface SurfaceTextureListener : TextureView.SurfaceTextureListener {
        fun changeParams(preset: BrushType.Preset, color: Color.Rgb)
    }
}

@Composable
fun rememberBrushPreviewRenderer(): BrushPreviewRenderer = remember { BrushPreviewRenderer() }