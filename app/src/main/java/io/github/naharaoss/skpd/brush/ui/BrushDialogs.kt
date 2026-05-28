package io.github.naharaoss.skpd.brush.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.ui.component.FancyDialog
import io.github.naharaoss.skpd.ui.component.IconPicker
import io.github.naharaoss.skpd.ui.component.PencilIconName
import io.github.naharaoss.skpd.ui.component.resourceIdFromNamedIcon

@Composable
fun TagEditDialog(
    processing: Boolean,
    title: @Composable () -> Unit,
    initialName: String,
    initialIcon: String?,
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, icon: String) -> Unit
) {
    var tagName by remember(initialName) { mutableStateOf(initialName) }
    var tagIcon by remember(initialIcon) { mutableStateOf(initialIcon ?: PencilIconName) }
    val focusRequester = remember { FocusRequester() }

    FancyDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.style_24px),
                contentDescription = "Tags"
            )
        },
        title = title,
        buttons = {
            TextButton(
                enabled = !processing,
                onClick = onDismissRequest
            ) {
                Text("Cancel")
            }

            TextButton(
                enabled = !processing && tagName.isNotEmpty(),
                onClick = { onConfirm(tagName, tagIcon) }
            ) {
                Text("Confirm")
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                tonalElevation = 16.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(Modifier.fillMaxWidth()) {
                    ToggleButton(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 16.dp),
                        checked = true,
                        onCheckedChange = {}
                    ) {
                        Icon(painterResource(resourceIdFromNamedIcon(tagIcon)), "Tag icon")
                        Spacer(Modifier.width(4.dp))
                        Text(tagName.ifEmpty { "Tag name" })
                    }
                }
            }

            TextField(
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("Tag name") },
            )

            IconPicker(
                icon = tagIcon,
                onIconSelect = { tagIcon = it }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun BrushMetadataEditDialog(
    processing: Boolean,
    title: @Composable () -> Unit,
    initialName: String,
    initialIcon: String?,
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, icon: String) -> Unit
) {
    var brushName by remember(initialName) { mutableStateOf(initialName) }
    var brushIcon by remember(initialIcon) { mutableStateOf(initialIcon ?: PencilIconName) }
    val focusRequester = remember { FocusRequester() }

    FancyDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.stylus_brush_24px),
                contentDescription = "Brush"
            )
        },
        title = title,
        buttons = {
            TextButton(
                enabled = !processing,
                onClick = onDismissRequest
            ) {
                Text("Cancel")
            }

            TextButton(
                enabled = !processing && brushName.isNotEmpty(),
                onClick = { onConfirm(brushName, brushIcon) }
            ) {
                Text("Confirm")
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BrushCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                preset = StampBrush.defaultPreset,
                favorite = false,
                selected = false,
                label = { Text(brushName.ifEmpty { "Brush name" }) },
                iconId = brushIcon,
                onBrushClick = {},
                onIconClick = {},
                onFavoriteChange = {}
            )

            TextField(
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                value = brushName,
                onValueChange = { brushName = it },
                label = { Text("Brush name") },
            )

            IconPicker(
                icon = brushIcon,
                onIconSelect = { brushIcon = it }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}