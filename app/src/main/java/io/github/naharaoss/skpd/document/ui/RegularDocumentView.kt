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
import kotlin.properties.Delegates

class RegularDocumentView(context: Context) : GLSurfaceView(context), DocumentViewInterface {
    private var _drawingBoardBackground = Color.Gray
    private var _document: DocumentAccess? = null
    private var _layer: DocumentAccess.Layer? = null
    private var _canvasTransform = Matrix()
    private var _brushPreset: BrushType.Preset? = null
    private var _brushColor = Color.Black

    private val renderer = Renderer(this)
    private val inputProcessor = object : InputProcessor(true, 10f) {
        override val width: Float get() = this@RegularDocumentView.width.toFloat()
        override val height: Float get() = this@RegularDocumentView.height.toFloat()
        override fun requestUnbufferedDispatch(event: MotionEvent) = this@RegularDocumentView.requestUnbufferedDispatch(event)
    }

    override var onTransformGesture: ((Matrix) -> Unit)? = null
    override var onTapGesture: ((fingers: Int) -> Unit)? = null

    override var drawingBoardBackground: Color
        get() = _drawingBoardBackground
        set(value) {
            if (_drawingBoardBackground == value) return
            _drawingBoardBackground = value
            requestRender()
        }

    override var fingerDrawing: Boolean
        get() = inputProcessor.fingerDrawing
        set(value) { inputProcessor.fingerDrawing = value }

    override var touchSlop: Float
        get() = inputProcessor.touchSlop
        set(value) { inputProcessor.touchSlop = value }

    override var document: DocumentAccess?
        get() = _document
        set(value) {
            if (_document == value) return
            _document = value

            if (value == null && _layer != null || value != null && !value.layers.contains(_layer)) {
                _layer = null
                queueEvent { renderer.setLayer(null) }
            }

            queueEvent { renderer.setDocument(value) }
            requestRender()
        }

    override var layer: DocumentAccess.Layer?
        get() = _layer
        set(value) {
            val document = _document
            if (_layer == value) return
            if (document == null || !document.layers.contains(value)) return
            _layer = value
            queueEvent { renderer.setLayer(value) }
            requestRender()
        }

    override var canvasTransform: Matrix
        get() = Matrix(_canvasTransform.values.clone())
        set(value) {
            if (_canvasTransform == value) return
            val value = Matrix(value.values.clone())
            _canvasTransform = value
            queueEvent { renderer.setCanvasTransform(value) }
            requestRender()
        }

    override var brushPreset: BrushType.Preset?
        get() = _brushPreset
        set(value) {
            if (_brushPreset == value) return
            _brushPreset = value
            queueEvent { renderer.setBrushPreset(value) }
            requestRender()
        }

    override var brushColor: Color
        get() = _brushColor
        set(value) {
            if (_brushColor == value) return
            _brushColor = value
            queueEvent { renderer.setBrushColor(value) }
            requestRender()
        }

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)

        for (action in inputProcessor.updateState(event)) {
            when (action) {
                is InputProcessor.Action.Stylus -> {
                    val clipPosition = Offset(x = action.input.x * 2f / width - 1f, y = 1f - action.input.y * 2f / height)
                    val worldToClip = Matrix().apply { scale(x = 2f / width, y = -2f / height) }
                    val clipToCanvas = Matrix()
                    clipToCanvas *= canvasTransform
                    clipToCanvas *= worldToClip
                    clipToCanvas.invert()
                    val canvasPosition = clipToCanvas.map(clipPosition)
                    val input = action.input.copy(x = canvasPosition.x, y = canvasPosition.y)

                    queueEvent {
                        when (action.kind) {
                            InputProcessor.Action.Stylus.Kind.Down -> {
                                renderer.beginStroke()
                                renderer.consumeInput(input)
                            }

                            InputProcessor.Action.Stylus.Kind.Move -> {
                                renderer.consumeInput(input)
                            }

                            InputProcessor.Action.Stylus.Kind.Up -> {
                                renderer.consumeInput(input)
                                renderer.endStroke()
                            }
                        }
                    }

                    requestRender()
                }

                is InputProcessor.Action.Cancel -> {
                    queueEvent { renderer.cancelStroke() }
                    requestRender()
                }

                is InputProcessor.Action.Transform -> {
                    onTransformGesture?.invoke(action.matrix)
                }

                is InputProcessor.Action.TapGesture -> {
                    onTapGesture?.invoke(action.fingers)
                }
            }
        }

        return true
    }

    private class Renderer(private val view: RegularDocumentView) : GLSurfaceView.Renderer {
        private var initialized = false
        private var width by Delegates.notNull<Int>()
        private var height by Delegates.notNull<Int>()
        private var canvasTransform = Matrix()
        private val viewport get() = Rect(offset = Offset(x = width / -2f, y = height / -2f), size = Size(width = width.toFloat(), height = height.toFloat()))
        private var documentRenderer: DocumentRenderer? = null
        private var layer: DocumentAccess.Layer? = null
        private var brushRenderer: BrushType.Renderer<BrushType.Preset>? = null

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            this.width = view.width
            this.height = view.height
            this.canvasTransform = view._canvasTransform
            this.layer = view._layer

            this.documentRenderer = view._document?.let { document ->
                val renderer = DocumentRenderer(document)
                renderer.update(viewport, canvasTransform)
                renderer
            }

            this.brushRenderer = view._brushPreset?.let { preset ->
                val renderer = preset.type.createTypeErasedRenderer()
                renderer.usePreset(preset)
                renderer.useColor(view._brushColor)
                renderer
            }

            this.initialized = true
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            this.width = width
            this.height = height
            this.documentRenderer?.update(viewport, canvasTransform)
        }

        override fun onDrawFrame(gl: GL10?) {
            val documentRenderer = documentRenderer
            val framebuffer = GLFramebuffer.default(width, height)

            if (documentRenderer == null) {
                framebuffer.bind {
                    setClearColor(view._drawingBoardBackground)
                    clear(GLFramebuffer.ClearType.Color)
                }
            } else {
                documentRenderer.render(
                    viewport = viewport,
                    canvasTransform = canvasTransform,
                    framebuffer = framebuffer,
                    background = view._drawingBoardBackground,
                    stencil = true
                )
            }
        }

        fun setDocument(document: DocumentAccess?) {
            if (!initialized) return
            if (this.documentRenderer?.document == document) return
            this.documentRenderer?.close()
            this.documentRenderer = document?.let {
                val renderer = DocumentRenderer(it)
                renderer.update(viewport, canvasTransform)
                renderer
            }
        }

        fun setLayer(layer: DocumentAccess.Layer?) {
            if (!initialized) return
            if (this.layer == layer) return
            val documentRenderer = documentRenderer ?: return
            documentRenderer.layers[layer]?.cancelBrush()
            this.layer = layer
        }

        fun setCanvasTransform(matrix: Matrix) {
            if (!initialized) return
            if (canvasTransform == matrix) return
            canvasTransform = matrix
            this.documentRenderer?.update(viewport, canvasTransform)
        }

        fun setBrushPreset(preset: BrushType.Preset?) {
            if (!initialized) return

            if (brushRenderer?.type != preset?.type) {
                brushRenderer?.close()
                brushRenderer = preset?.type?.createTypeErasedRenderer()?.also { renderer ->
                    renderer.usePreset(preset)
                    renderer.useColor(view._brushColor)
                }
            } else if (brushRenderer != null && preset != null) {
                brushRenderer?.usePreset(preset)
            }
        }

        fun setBrushColor(color: Color) {
            if (!initialized) return
            brushRenderer?.useColor(color)
        }

        fun beginStroke() {
            val brushRenderer = brushRenderer ?: return
            brushRenderer.beginStroke()
        }

        fun consumeInput(input: StylusInput) {
            val documentRenderer = documentRenderer ?: return
            val brushRenderer = brushRenderer ?: return
            val layer = layer ?: return
            val layerRenderer = documentRenderer.layers[layer] ?: return
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
            val layer = layer ?: return
            val layerRenderer = documentRenderer.layers[layer] ?: return

            documentRenderer.document.openWriter().use { writer ->
                with(layerRenderer) {
                    writer.commitBrush()
                }
            }

            brushRenderer.endStroke()
        }

        fun cancelStroke() {
            val documentRenderer = documentRenderer ?: return
            val brushRenderer = brushRenderer ?: return
            val layer = layer ?: return
            val layerRenderer = documentRenderer.layers[layer] ?: return
            layerRenderer.cancelBrush()
            brushRenderer.endStroke()
        }
    }
}