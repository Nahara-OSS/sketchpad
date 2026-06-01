package io.github.naharaoss.skpd.document.ui

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.InputProcessor
import io.github.naharaoss.skpd.brush.StylusInput
import io.github.naharaoss.skpd.document.DocumentAccess
import io.github.naharaoss.skpd.document.graphics.DocumentRenderer
import io.github.naharaoss.skpd.utils.GLFramebuffer
import io.github.naharaoss.skpd.utils.calculateVisibleTiles
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class DocumentView(context: Context) : GLSurfaceView(context) {
    private val inputProcessor = object : InputProcessor(fingerDrawing = true, touchSlop = 10f) {
        override val width: Float get() = this@DocumentView.width.toFloat()
        override val height: Float get() = this@DocumentView.height.toFloat()
        override fun requestUnbufferedDispatch(event: MotionEvent) = this@DocumentView.requestUnbufferedDispatch(event)
    }

    private val renderer: Renderer = Renderer(this)
    private var _document: DocumentAccess? = null
    private val _canvasTransform = Matrix()
    private var _brushPreset: BrushType.Preset? = null
    private var _brushColor = Color.Black

    var fingerDrawing
        get() = inputProcessor.fingerDrawing
        set(value) { inputProcessor.fingerDrawing = value }

    var touchSlop
        get() = inputProcessor.touchSlop
        set(value) { inputProcessor.touchSlop = value }

    var document
        get() = _document
        set(value) {
            if (_document == value) return
            _document = value
            queueEvent { renderer.setDocument(value) }
            requestRender()
        }

    var canvasTransform
        get() = Matrix(_canvasTransform.values.clone())
        set(value) {
            val value = Matrix(value.values.clone())
            if (value == _canvasTransform) return
            _canvasTransform.setFrom(value)
            queueEvent { renderer.setCanvasTransform(value) }
            requestRender()
        }

    var brushPreset
        get() = _brushPreset
        set(value) {
            if (_brushPreset == value) return
            _brushPreset = value
            queueEvent { renderer.setBrushPreset(value) }
            requestRender()
        }

    var brushColor
        get() = _brushColor
        set(value) {
            if (_brushColor == value) return
            _brushColor = value
            queueEvent { renderer.setBrushColor(value) }
            requestRender()
        }

    var onTransform: ((amount: Matrix) -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val document = _document
        if (event == null || document == null) return super.onTouchEvent(event)

        for (action in inputProcessor.updateState(event)) {
            when (action) {
                is InputProcessor.Action.Stylus -> {
                    queueEvent {
                        if (action.kind == InputProcessor.Action.Stylus.Kind.Down) renderer.beginStroke()
                        renderer.consumeInput(action.input)
                        if (action.kind == InputProcessor.Action.Stylus.Kind.Up) renderer.endStroke()
                    }

                    requestRender()
                }

                is InputProcessor.Action.Cancel -> {
                    queueEvent { renderer.cancelStroke() }
                    requestRender()
                }

                is InputProcessor.Action.Transform -> {
                    val onTransform = onTransform
                    if (onTransform != null) onTransform(action.matrix)
                }

                else -> {}
            }
        }

        return true
    }

    private class Renderer(private val view: DocumentView) : GLSurfaceView.Renderer {
        private var documentRenderer: DocumentRenderer? = null
        private var brushRenderer: BrushType.Renderer<BrushType.Preset>? = null
        private var brushColor = Color.Black
        private val canvasTransform = Matrix()
        private var width = -1
        private var height = -1
        private val viewport get() = Rect(offset = Offset(x = width / -2f, y = height / -2f), size = Size(width = width.toFloat(), height = height.toFloat()))
        var initialized = false

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            initialized = true
            setDocument(view._document)
            setCanvasTransform(view._canvasTransform)
            setBrushPreset(view._brushPreset)
            setBrushColor(view._brushColor)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            this.width = width
            this.height = height
            documentRenderer?.update(viewport, canvasTransform)
        }

        override fun onDrawFrame(gl: GL10?) {
            GLFramebuffer.default(width, height).bind {
                setViewport(0, 0, width, height)
                setClearColor(Color.White)
                clear(GLFramebuffer.ClearType.Color)
            }

            documentRenderer?.render(
                viewport = viewport,
                canvasTransform = canvasTransform,
                framebuffer = GLFramebuffer.default(width, height)
            )
        }

        fun setDocument(document: DocumentAccess?) {
            if (!initialized) return
            documentRenderer?.close()
            documentRenderer = document?.let {
                val renderer = DocumentRenderer(it)
                if (width != -1 && height != -1) renderer.update(viewport, canvasTransform)
                renderer
            }
        }

        fun setCanvasTransform(value: Matrix) {
            if (!initialized) return
            canvasTransform.setFrom(value)
            if (width != -1 && height != -1) documentRenderer?.update(viewport, canvasTransform)
        }

        fun setBrushPreset(preset: BrushType.Preset?) {
            if (!initialized) return
            val brushRenderer = brushRenderer

            if (preset != null && brushRenderer?.type == preset.type) {
                brushRenderer.usePreset(preset)
            } else {
                this.brushRenderer?.close()
                this.brushRenderer = preset?.let {
                    val renderer = it.type.createTypeErasedRenderer()
                    renderer.usePreset(it)
                    renderer.useColor(brushColor)
                    renderer
                }
            }
        }

        fun setBrushColor(color: Color) {
            if (!initialized) return
            brushColor = color
            brushRenderer?.useColor(color)
        }

        fun beginStroke() {
            val documentRenderer = documentRenderer ?: return
            val brushRenderer = brushRenderer ?: return
            brushRenderer.beginStroke()
        }

        fun consumeInput(input: StylusInput) {
            val documentRenderer = documentRenderer ?: return
            val brushRenderer = brushRenderer ?: return
            val layer = documentRenderer.document.layers.first()
            val layerRenderer = documentRenderer.layers[layer] ?: return
            val clipPosition = Offset(x = input.x * 2f / width - 1f, y = 1f - input.y * 2f / height)
            val worldToClip = Matrix().apply { scale(x = 2f / width, y = -2f / height) }

            val clipToCanvas = Matrix()
            clipToCanvas *= canvasTransform
            clipToCanvas *= worldToClip
            clipToCanvas.invert()

            val canvasPosition = clipToCanvas.map(clipPosition)
            val input = input.copy(x = canvasPosition.x, y = canvasPosition.y)
            val affectedRect = brushRenderer.consumeInput(input)
            val affectedTiles = calculateVisibleTiles(
                viewport = affectedRect,
                canvasSize = documentRenderer.document.size,
                canvasTransform = Matrix(),
                tileSize = documentRenderer.document.tileSize
            )

            for (address in affectedTiles) {
                layerRenderer.useBrush(address, brushRenderer)
            }
        }

        fun endStroke() {
            val documentRenderer = documentRenderer ?: return
            val brushRenderer = brushRenderer ?: return
            val layer = documentRenderer.document.layers.first()
            val layerRenderer = documentRenderer.layers[layer] ?: return

            documentRenderer.document.openWriter().use { writer ->
                with(layerRenderer) { writer.commitBrush() }
            }

            brushRenderer.endStroke()
        }

        fun cancelStroke() {
            val documentRenderer = documentRenderer ?: return
            val brushRenderer = brushRenderer ?: return
            val layer = documentRenderer.document.layers.first()
            val layerRenderer = documentRenderer.layers[layer] ?: return
            layerRenderer.cancelBrush()
            brushRenderer.endStroke()
        }
    }
}