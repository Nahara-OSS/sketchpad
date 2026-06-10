package io.github.naharaoss.skpd.document

import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.document.ui.RegularDocumentView
import io.github.naharaoss.skpd.toolbar.ToolbarViewModel
import io.github.naharaoss.skpd.toolbar.ui.ToolbarOverlay
import io.github.naharaoss.skpd.ui.component.FancyDialog
import io.github.naharaoss.skpd.ui.component.FancyDialogText
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme

/**
 * The activity for Sketchpad documents. Open this activity with document UID to open existing
 * document. Exit with error if there is no document with given UID.
 */
@AndroidEntryPoint
class DocumentActivity : ComponentActivity() {
    private val brushListViewModel: BrushListViewModel by viewModels()
    private val toolbarViewModel: ToolbarViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalGridApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val documentRef = when {
            intent.hasExtra(EXTRA_DOCUMENT_ID) -> DocumentViewModel.DocumentRef.Local(intent.getLongExtra(EXTRA_DOCUMENT_ID, -1))
            else -> {
                setContent {
                    SketchpadTheme {
                        FancyDialog(
                            onDismissRequest = { finish() },
                            icon = { Icon(painterResource(R.drawable.question_mark_24px), null) },
                            title = { Text("No document") }
                        ) {
                            FancyDialogText {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (line in stringArrayResource(R.array.document_no_ref_content)) {
                                        Text(line)
                                    }
                                }
                            }
                        }
                    }
                }

                return
            }
        }

        window.isNavigationBarContrastEnforced = false
        window.insetsController!!.hide(WindowInsets.Type.navigationBars())

        setContent {
            val documentViewModel = hiltViewModel(creationCallback = { factory: DocumentViewModel.Factory -> factory.create(documentRef) })
            val windowSizeClass = calculateWindowSizeClass(this)
            var showBrushList by remember { mutableStateOf(false) }

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
                    undockedPadding = 8.dp,
                    toolbarViewModel = toolbarViewModel,
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