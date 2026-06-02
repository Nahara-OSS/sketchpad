package io.github.naharaoss.skpd.document

import android.content.Intent
import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushEditorActivity
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.ui.BrushPicker
import io.github.naharaoss.skpd.document.ui.RegularDocumentView
import io.github.naharaoss.skpd.settings.SettingsViewModel
import io.github.naharaoss.skpd.ui.component.resourceIdFromNamedIcon
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme
import kotlinx.coroutines.launch

/**
 * The activity for Sketchpad documents. Open this activity with document UID to open existing
 * document. Exit with error if there is no document with given UID.
 */
@AndroidEntryPoint
class DocumentActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val brushListViewModel: BrushListViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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
            val document by documentViewModel.document.collectAsState()
            val activeLayer by documentViewModel.activeLayer.collectAsState()

            val settings by settingsViewModel.settings.collectAsState()
            val windowSizeClass = calculateWindowSizeClass(this)
            var transform by remember { mutableStateOf(Matrix()) }
            val brushes by brushListViewModel.brushes.collectAsState()
            var selectedBrush by remember(brushes.isNullOrEmpty()) { mutableStateOf(brushes?.firstOrNull()) }
            var selectedBrushIcon: String? by remember { mutableStateOf(null) }
            var selectedBrushPreset: BrushType.Preset? by remember { mutableStateOf(null) }
            var showBrushList by remember { mutableStateOf(false) }

            LaunchedEffect(selectedBrush) {
                when (val selectedBrush = selectedBrush) {
                    null -> {
                        selectedBrushIcon = null
                        selectedBrushPreset = null
                    }

                    else -> {
                        launch {
                            selectedBrush.icon.collect { icon -> selectedBrushIcon = icon }
                        }

                        launch {
                            selectedBrush.preset.collect { preset -> selectedBrushPreset = preset }
                        }
                    }
                }
            }

            BackHandler(enabled = showBrushList) {
                showBrushList = false
            }

            SketchpadTheme {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { RegularDocumentView(it) }
                ) {
                    it.document = document
                    it.layer = activeLayer
                    it.brushPreset = selectedBrushPreset
                    it.brushColor = Color.Black
                    it.canvasTransform = transform
                    it.fingerDrawing = settings.input.fingerDrawing
                    it.onTransformGesture = { matrix ->
                        val newTransform = Matrix(transform.values.clone())
                        newTransform *= matrix
                        transform = newTransform
                    }
                }

                Column {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 2.dp
                    ) {
                        Row {
                            IconButton({ finish() }) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_back_24px),
                                    contentDescription = "Go back"
                                )
                            }

                            FilledIconToggleButton(
                                checked = showBrushList,
                                onCheckedChange = { showBrushList = it }
                            ) {
                                val selectedBrushIcon = selectedBrushIcon

                                Icon(
                                    painter = painterResource(if (selectedBrushIcon != null) resourceIdFromNamedIcon(selectedBrushIcon) else R.drawable.question_mark_24px),
                                    contentDescription = "Brush"
                                )
                            }
                        }
                    }

                    Box(Modifier.padding(16.dp)) {
                        AnimatedContent(
                            modifier = Modifier.width(if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) 380.dp else 500.dp),
                            targetState = showBrushList,
                            transitionSpec = {
                                val enter = fadeIn() + slideInHorizontally { if (targetState) -it else it }
                                val exit = fadeOut() + slideOutHorizontally { if (targetState) it else -it }
                                (enter togetherWith exit).using(sizeTransform = SizeTransform(clip = false))
                            }
                        ) { show ->
                            when (show) {
                                true -> Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    var query by remember { mutableStateOf("") }

                                    Column {
                                        Row(
                                            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    val intent = Intent(this@DocumentActivity, BrushEditorActivity::class.java)
                                                    startActivity(intent)
                                                }
                                            ) {
                                                Icon(painterResource(R.drawable.edit_24px), "Edit brushes")
                                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                                Text("Edit")
                                            }
                                        }

                                        BrushPicker(
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                            compact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact,
                                            padding = PaddingValues(16.dp),
                                            brushes = brushes ?: emptyList(),
                                            onBrushSelect = {
                                                selectedBrush = it
                                                showBrushList = false
                                            }
                                        )
                                    }
                                }

                                false -> Spacer(Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_DOCUMENT_ID = "documentId"
    }
}