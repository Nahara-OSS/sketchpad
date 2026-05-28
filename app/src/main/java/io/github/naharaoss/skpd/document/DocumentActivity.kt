package io.github.naharaoss.skpd.document

import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalWindowInfo
import io.github.naharaoss.skpd.settings.Toolbar
import io.github.naharaoss.skpd.toolbar.ToolbarsOverlay
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme
import io.github.naharaoss.skpd.utils.calculateVisibleTiles

/**
 * The activity for Sketchpad documents. Open this activity with document UID to open existing
 * document. Exit with error if there is no document with given UID.
 */
class DocumentActivity : ComponentActivity() {
    companion object {
        /**
         * UID of the document to open. Must be supplied.
         */
        val DOCUMENT_ID = "documentId"

        /**
         * Whether the document should be opened with read-only mode. Default value is `false`.
         */
        val READ_ONLY = "readOnly"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.isNavigationBarContrastEnforced = false
        window.insetsController!!.hide(WindowInsets.Type.navigationBars())

        val documentId = intent.getStringExtra(DOCUMENT_ID)
        val readOnly = intent.getBooleanExtra(READ_ONLY, false)
        val displayCutout = display.cutout

        setContent {
            val toolbars = listOf(
                Toolbar(
                    docked = true,
                    side = Toolbar.Side.Top,
                    align = Toolbar.Align.Start,
                    tools = listOf(
                        Toolbar.Tool.Back,
                        Toolbar.Tool.Undo,
                        Toolbar.Tool.Redo
                    )
                ),
                Toolbar(
                    docked = true,
                    side = Toolbar.Side.Top,
                    align = Toolbar.Align.End,
                    tools = listOf(
                        Toolbar.Tool.Layers,
                        Toolbar.Tool.Menu
                    )
                ),
                Toolbar(
                    docked = false,
                    side = Toolbar.Side.Start,
                    align = Toolbar.Align.Middle,
                    tools = listOf(
                        Toolbar.Tool.Brush,
                        Toolbar.Tool.Brush,
                        Toolbar.Tool.Brush,
                        Toolbar.Tool.ColorPicker,
                        Toolbar.Tool.ColorSampler,
                    )
                )
            )

            var transform by remember { mutableStateOf(Matrix()) }
            val windowSize = LocalWindowInfo.current.containerSize

            SketchpadTheme {
                Box(Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(rememberTransformableState { centroid, zoomChange, panChange, rotationChange ->
                                val newTransform = Matrix(transform.values.clone())
                                val mat = Matrix()
                                mat.translate(x = centroid.x - windowSize.width / 2, y = centroid.y - windowSize.height / 2)
                                mat.translate(x = panChange.x, y = panChange.y)
                                mat.rotateZ(rotationChange)
                                mat.scale(x = zoomChange, y = zoomChange)
                                mat.translate(x = -(centroid.x - windowSize.width / 2), y = -(centroid.y - windowSize.height / 2))

                                // Calculating scale level
                                val a = newTransform.map(Offset(x = 0f, y = 0f))
                                val b = newTransform.map(Offset(x = 1f, y = 0f))
                                val scale = (b - a).getDistance()

                                if (scale < 0.2f) {
                                    val amount = 0.2f / scale
                                    mat.translate(x = centroid.x - windowSize.width / 2, y = centroid.y - windowSize.height / 2)
                                    mat.scale(x = amount, y = amount)
                                    mat.translate(x = -(centroid.x - windowSize.width / 2), y = -(centroid.y - windowSize.height / 2))
                                }

                                newTransform *= mat
                                transform = newTransform
                            })
                    ) {
                        withTransform({ translate(size.width / 2f, size.height / 2f) }) {
                            withTransform({ transform(transform) }) {
                                calculateVisibleTiles(
                                    viewport = Rect(
                                        offset = Offset(x = size.width / -2f, y = size.height / -2f),
                                        size = size
                                    ),
                                    canvasSize = io.github.naharaoss.skpd.utils.Size.Infinite,
                                    canvasTransform = transform,
                                    tileSize = 128
                                ).forEach { (x, y, z) ->
                                    val red = (16 + x) / 32f
                                    val green = (16 + y) / 32f

                                    drawRect(
                                        color = Color(red, green, 0.5f, 1f),
                                        topLeft = Offset(x = x * 128f, y = y * 128f),
                                        size = Size(width = 128f, height = 128f)
                                    )
                                }
                            }
                        }
                    }

                    ToolbarsOverlay(
                        modifier = Modifier.fillMaxSize(),
                        toolbars = toolbars,
                        displayCutout = displayCutout
                    )
                }
            }
        }
    }
}