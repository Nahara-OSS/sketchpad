package io.github.naharaoss.skpd.document

import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.document.ui.RegularDocumentView
import io.github.naharaoss.skpd.toolbar.Tool
import io.github.naharaoss.skpd.toolbar.Toolbar
import io.github.naharaoss.skpd.toolbar.ui.ToolbarOverlay
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme
import java.util.UUID

/**
 * The activity for Sketchpad documents. Open this activity with document UID to open existing
 * document. Exit with error if there is no document with given UID.
 */
@AndroidEntryPoint
class DocumentActivity : ComponentActivity() {
    private val brushListViewModel: BrushListViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalGridApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val documentRef = when {
            intent.hasExtra(EXTRA_DOCUMENT_ID) -> DocumentViewModel.DocumentRef.Local(intent.getLongExtra(EXTRA_DOCUMENT_ID, -1))
            else -> throw Exception("Don't know where to open document")
        }

        window.isNavigationBarContrastEnforced = false
        window.insetsController!!.hide(WindowInsets.Type.navigationBars())

        setContent {
            val documentViewModel = hiltViewModel(creationCallback = { factory: DocumentViewModel.Factory -> factory.create(documentRef) })
            val windowSizeClass = calculateWindowSizeClass(this)
            var showBrushList by remember { mutableStateOf(false) }

            var toolbars by rememberSerializable {
                mutableStateOf(listOf(
                    Toolbar(
                        id = UUID.randomUUID(),
                        side = Toolbar.Side.Top,
                        position = Toolbar.Position.Start,
                        docked = true,
                        tools = listOf(
                            Tool.Exit
                        )
                    ),
                    Toolbar(
                        id = UUID.randomUUID(),
                        side = Toolbar.Side.Top,
                        position = Toolbar.Position.End,
                        docked = true,
                        tools = listOf(
                            Tool.ResetTransform,
                            Tool.Layers
                        )
                    ),
                    Toolbar(
                        id = UUID.randomUUID(),
                        side = Toolbar.Side.Left,
                        position = Toolbar.Position.Center,
                        docked = false,
                        tools = listOf(
                            Tool.Brush(null),
                            Tool.Brush(null),
                            Tool.Brush(null)
                        )
                    )
                ))
            }

            BackHandler(enabled = showBrushList) {
                showBrushList = false
            }

            SketchpadTheme {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { RegularDocumentView(it).also(documentViewModel::setView) }
                )

                ToolbarOverlay(
                    modifier = Modifier.fillMaxSize(),
                    toolbars = toolbars,
                    onToolbarsChange = { toolbars = it },
                    undockedPadding = 8.dp,
                    documentViewModel = documentViewModel,
                    brushListViewModel = brushListViewModel,
                    windowSizeClass = windowSizeClass,
                    onCloseDocument = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_DOCUMENT_ID = "documentId"
    }
}