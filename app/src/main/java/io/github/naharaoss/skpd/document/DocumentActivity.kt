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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
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
import io.github.naharaoss.skpd.brush.ui.BrushPicker
import io.github.naharaoss.skpd.document.ui.DocumentViewInterface
import io.github.naharaoss.skpd.document.ui.RegularDocumentView
import io.github.naharaoss.skpd.resource.BrushResource
import io.github.naharaoss.skpd.settings.SettingsViewModel
import io.github.naharaoss.skpd.ui.component.resourceIdFromNamedIcon
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The activity for Sketchpad documents. Open this activity with document UID to open existing
 * document. Exit with error if there is no document with given UID.
 */
@AndroidEntryPoint
class DocumentActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val brushListViewModel: BrushListViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class,
        ExperimentalMaterial3ExpressiveApi::class
    )
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
            val layers by documentViewModel.layers.collectAsState()
            val activeLayer by documentViewModel.activeLayer.collectAsState()
            var transform by remember { mutableStateOf(Matrix()) }
            val brushes by brushListViewModel.brushes.collectAsState()
            var selectedBrush by remember { mutableStateOf<BrushResource?>(null) }
            var selectedBrushPreset by remember(selectedBrush) { mutableStateOf(selectedBrush?.preset?.replayCache?.lastOrNull()) }
            var selectedBrushIcon by remember(selectedBrush) { mutableStateOf(selectedBrush?.icon?.replayCache?.lastOrNull()) }
            var selectedBrushColor by remember { mutableStateOf(Color.Black) }
            val settings by settingsViewModel.settings.collectAsState()
            val windowSizeClass = calculateWindowSizeClass(this)
            var showBrushList by remember { mutableStateOf(false) }
            var showLayerList by remember { mutableStateOf(false) }
            var view by remember { mutableStateOf<DocumentViewInterface?>(null) }

            LaunchedEffect(brushes != null) {
                if (selectedBrush == null) {
                    brushes?.let { selectedBrush = it.firstOrNull() }
                }
            }

            LaunchedEffect(selectedBrush) {
                when (val selectedBrush = selectedBrush) {
                    null -> {
                        selectedBrushPreset = null
                        selectedBrushIcon = null
                    }

                    else -> {
                        launch { selectedBrush.preset.collect { selectedBrushPreset = it } }
                        launch { selectedBrush.icon.collect { selectedBrushIcon = it } }
                    }
                }
            }

            LaunchedEffect(documentViewModel.changed) {
                documentViewModel.changed.collect { view?.triggerDocumentUpdate() }
            }

            BackHandler(enabled = showBrushList) {
                showBrushList = false
            }

            SketchpadTheme {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { RegularDocumentView(it) }
                ) {
                    view = it
                    it.document = document
                    it.layer = activeLayer
                    it.brushPreset = selectedBrushPreset
                    it.brushColor = selectedBrushColor
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
                        Row(Modifier.padding(4.dp)) {
                            IconButton({ finish() }) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_back_24px),
                                    contentDescription = "Go back"
                                )
                            }

                            FilledIconToggleButton(
                                checked = showBrushList,
                                onCheckedChange = {
                                    showBrushList = it
                                    showLayerList = false
                                }
                            ) {
                                val selectedBrushIcon = selectedBrushIcon

                                Icon(
                                    painter = painterResource(if (selectedBrushIcon != null) resourceIdFromNamedIcon(selectedBrushIcon) else R.drawable.question_mark_24px),
                                    contentDescription = "Brush"
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            FilledIconToggleButton(
                                checked = showLayerList,
                                onCheckedChange = {
                                    showBrushList = false
                                    showLayerList = it
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.layers_24px),
                                    contentDescription = "Layers"
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

                        AnimatedContent(
                            modifier = Modifier
                                .width(300.dp)
                                .fillMaxHeight(0.5f)
                                .heightIn(min = 200.dp)
                                .align(Alignment.TopEnd),
                            targetState = showLayerList,
                            transitionSpec = {
                                val enter = fadeIn() + slideInHorizontally { if (targetState) it else -it }
                                val exit = fadeOut() + slideOutHorizontally { if (targetState) -it else it }
                                (enter togetherWith exit).using(sizeTransform = SizeTransform(clip = false))
                            }
                        ) { show ->
                            when (show) {
                                true -> Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.padding(start = 16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                                                Text("Layers")
                                            }

                                            IconButton({ documentViewModel.addLayer() }) {
                                                Icon(
                                                    painter = painterResource(R.drawable.add_24px),
                                                    contentDescription = "Add layer"
                                                )
                                            }
                                        }

                                        HorizontalDivider(Modifier.fillMaxWidth())

                                        LazyColumn(Modifier.fillMaxWidth()) {
                                            items(layers.size, key = { layers[layers.size - 1 - it].id }) { index ->
                                                val layer = layers[layers.size - 1 - index]
                                                var visible by remember { mutableStateOf(layer.visible) }

                                                LaunchedEffect(documentViewModel.changed) {
                                                    documentViewModel.changed.collect { visible = layer.visible }
                                                }

                                                SegmentedListItem(
                                                    onClick = {
                                                        if (activeLayer == layer) return@SegmentedListItem
                                                        documentViewModel.setActiveLayer(layer)
                                                    },
                                                    shapes = ListItemDefaults.segmentedShapes(index, layers.size),
                                                    selected = activeLayer == layer,
                                                    content = { Text(layer.name) },
                                                    supportingContent = {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                modifier = Modifier.size(12.dp),
                                                                painter = painterResource(R.drawable.opacity_24px),
                                                                contentDescription = "Opacity"
                                                            )
                                                            Text("${(layer.opacity * 100f).roundToInt()}%")
                                                            Text("\u2022")
                                                            Text("Normal")
                                                        }
                                                    },
                                                    trailingContent = {
                                                        Row {
                                                            IconButton({ documentViewModel.setLayerVisibility(layer, !layer.visible) }) {
                                                                Icon(
                                                                    painter = painterResource(if (visible) R.drawable.visibility_24px else R.drawable.visibility_off_24px),
                                                                    contentDescription = "Toggle visibility"
                                                                )
                                                            }

                                                            IconButton({ documentViewModel.deleteLayer(layer) }) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.delete_24px),
                                                                    contentDescription = "Delete layer"
                                                                )
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
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