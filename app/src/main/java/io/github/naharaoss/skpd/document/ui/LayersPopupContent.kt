package io.github.naharaoss.skpd.document.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.document.DocumentViewModel
import io.github.naharaoss.skpd.ui.component.FancyDialog
import io.github.naharaoss.skpd.utils.BlendMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LayersPopupContent(
    modifier: Modifier = Modifier,
    documentViewModel: DocumentViewModel
) {
    val layers by documentViewModel.layers.collectAsState()
    val activeLayer by documentViewModel.activeLayer.collectAsState()
    var editingLayerId: Any? by remember { mutableStateOf(null) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton({ documentViewModel.addLayer() }) {
                    Icon(
                        painter = painterResource(R.drawable.add_24px),
                        contentDescription = "Add layer"
                    )
                }
            }

            HorizontalDivider(Modifier.fillMaxWidth())

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                items(layers.size, key = { layers[layers.size - 1 - it].id }) { index ->
                    val layer = layers[layers.size - 1 - index]

                    SegmentedListItem(
                        modifier = Modifier.animateItem(),
                        onClick = {
                            if (activeLayer == layer) {
                                editingLayerId = if (editingLayerId == layer.id) null else layer.id
                            } else {
                                documentViewModel.setActiveLayer(layer)
                            }
                        },
                        onLongClick = { editingLayerId = if (editingLayerId == layer.id) null else layer.id },
                        shapes = ListItemDefaults.segmentedShapes(index, layers.size + 1),
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
                                Text(layer.blend.name)
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton({ documentViewModel.editLayer(layer, visible = !layer.visible) }) {
                                    Icon(
                                        painter = painterResource(if (layer.visible) R.drawable.visibility_24px else R.drawable.visibility_off_24px),
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

                    AnimatedVisibility(
                        visible = editingLayerId == layer.id,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        var showName by remember(editingLayerId == layer.id) { mutableStateOf(false) }
                        var showOpacitySlider by remember(editingLayerId == layer.id) { mutableStateOf(false) }
                        var showBlendModes by remember(editingLayerId == layer.id) { mutableStateOf(false) }

                        Column(
                            modifier = Modifier.padding(top = ListItemDefaults.SegmentedGap),
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            SegmentedListItem(
                                onClick = { showName = !showName },
                                selected = showName,
                                shapes = ListItemDefaults.segmentedShapes(0, 3),
                                colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                content = { Text("Layer name") },
                                supportingContent = { Text(layer.name) },
                                leadingContent = { Icon(painterResource(R.drawable.edit_24px), null) }
                            )

                            SegmentedListItem(
                                onClick = { showOpacitySlider = !showOpacitySlider },
                                selected = showOpacitySlider,
                                shapes = ListItemDefaults.segmentedShapes(1, 3),
                                colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                content = { Text("Opacity") },
                                supportingContent = { Text("${(layer.opacity * 100f).roundToInt()}%") },
                                leadingContent = { Icon(painterResource(R.drawable.opacity_24px), null) },
                                trailingContent = {
                                    if (showOpacitySlider) {
                                        var opacity by remember(layer.opacity) { mutableFloatStateOf(layer.opacity) }

                                        Popup(
                                            onDismissRequest = { showOpacitySlider = false },
                                            properties = PopupProperties(focusable = true)
                                        ) {
                                            Surface(
                                                shadowElevation = 2.dp,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Slider(
                                                    modifier = Modifier
                                                        .widthIn(max = 300.dp)
                                                        .padding(16.dp),
                                                    value = opacity,
                                                    onValueChange = { opacity = it },
                                                    onValueChangeFinished = { documentViewModel.editLayer(layer, opacity = opacity) }
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            SegmentedListItem(
                                onClick = { showBlendModes = !showBlendModes },
                                selected = showBlendModes,
                                shapes = ListItemDefaults.segmentedShapes(2, 3),
                                colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                content = { Text("Blend mode") },
                                supportingContent = { Text(layer.blend.name) },
                                leadingContent = { Icon(painterResource(R.drawable.opacity_24px), null) },
                                trailingContent = {
                                    DropdownMenu(
                                        expanded = showBlendModes,
                                        onDismissRequest = { showBlendModes = false }
                                    ) {
                                        for (mode in BlendMode.entries) {
                                            DropdownMenuItem(
                                                text = { Text(mode.name) },
                                                onClick = {
                                                    documentViewModel.editLayer(layer, blend = mode)
                                                    showBlendModes = false
                                                }
                                            )
                                        }
                                    }
                                }
                            )

                            if (showName) {
                                var name by remember { mutableStateOf(layer.name) }
                                val focusRequester = remember { FocusRequester() }

                                fun onDone() {
                                    if (name.isBlank()) return
                                    documentViewModel.editLayer(layer, name = name)
                                    showName = false
                                }

                                FancyDialog(
                                    onDismissRequest = { showName = false },
                                    icon = { Icon(painterResource(R.drawable.edit_24px), "Edit layer name") },
                                    title = { Text("Edit layer name") },
                                    buttons = {
                                        TextButton({ showName = false }) {
                                            Text("Cancel")
                                        }

                                        TextButton(
                                            enabled = name.isNotBlank(),
                                            onClick = { onDone() }
                                        ) {
                                            Text("Confirm")
                                        }
                                    }
                                ) {
                                    TextField(
                                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Layer name") },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { onDone() })
                                    )
                                }

                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                }
                            }
                        }
                    }
                }

                if (layers.isEmpty()) {
                    item("(empty)") {
                        SegmentedListItem(
                            modifier = Modifier.animateItem(),
                            onClick = { documentViewModel.addLayer() },
                            shapes = ListItemDefaults.segmentedShapes(0, 2),
                            colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            content = { Text("No layers") },
                            supportingContent = { Text("Tap to add layer") }
                        )
                    }
                }

                item("(background)") {
                    SegmentedListItem(
                        modifier = Modifier.animateItem(),
                        onClick = {},
                        shapes = ListItemDefaults.segmentedShapes(1, 2),
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        content = { Text("Background") },
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
                                Text("100%")
                                Text("\u2022")
                                Text("#FFFFFF")
                            }
                        },
                        trailingContent = {
                            IconButton({}) {
                                Box(Modifier.size(24.dp).background(color = Color.White, shape = CircleShape))
                            }
                        }
                    )
                }
            }
        }
    }
}