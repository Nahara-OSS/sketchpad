package io.github.naharaoss.skpd.document

import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.AndroidEntryPoint
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.document.ui.DocumentView
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme

/**
 * The activity for Sketchpad documents. Open this activity with document UID to open existing
 * document. Exit with error if there is no document with given UID.
 */
@AndroidEntryPoint
class DocumentActivity : ComponentActivity() {
    private val documentViewModel: DocumentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.isNavigationBarContrastEnforced = false
        window.insetsController!!.hide(WindowInsets.Type.navigationBars())

        setContent {
            var transform by remember { mutableStateOf(Matrix()) }
            val windowSize = LocalWindowInfo.current.containerSize

            SketchpadTheme {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { DocumentView(it) }
                ) {
                    it.document = documentViewModel.document
                    it.brushPreset = StampBrush.defaultPreset
                    it.brushColor = Color.Black
                }
            }
        }
    }
}