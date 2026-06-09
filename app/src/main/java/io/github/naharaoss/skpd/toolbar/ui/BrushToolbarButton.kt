package io.github.naharaoss.skpd.toolbar.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushEditorActivity
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.brush.ui.BrushPicker
import io.github.naharaoss.skpd.document.DocumentViewModel
import io.github.naharaoss.skpd.resource.BrushItem
import io.github.naharaoss.skpd.ui.component.SketchpadPopup
import io.github.naharaoss.skpd.ui.component.SketchpadPopupTitleBar
import io.github.naharaoss.skpd.ui.component.resourceIdFromNamedIcon

@Composable
fun BrushToolbarButton(
    modifier: Modifier = Modifier,
    brush: BrushItem?,
    compact: Boolean,
    documentViewModel: DocumentViewModel,
    brushListViewModel: BrushListViewModel,
    onBrushSelect: (BrushItem) -> Unit
) {
    val selectedBrush by documentViewModel.brush.collectAsState()
    var showBrushPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier) {
        FilledIconToggleButton(
            checked = brush != null && brush.id == selectedBrush?.id,
            onCheckedChange = {
                when {
                    !it -> showBrushPicker = true
                    brush != null && selectedBrush?.id != brush.id -> documentViewModel.setBrush(
                        brush
                    )
                }
            }
        ) {
            Icon(
                painter = painterResource(resourceIdFromNamedIcon(brush?.icon ?: "")),
                contentDescription = brush?.name ?: "Brush"
            )
        }

        SketchpadPopup(
            modifier = Modifier
                .widthIn(max = if (compact) 400.dp else 600.dp)
                .heightIn(min = 200.dp)
                .fillMaxHeight(0.8f),
            visible = showBrushPicker,
            onDismissRequest = { showBrushPicker = false },
            titleBar = {
                SketchpadPopupTitleBar(
                    label = { Text("Brush picker") },
                    buttons = {
                        IconButton({
                            val intent = Intent(context, BrushEditorActivity::class.java)
                            context.startActivity(intent)
                            showBrushPicker = false
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.edit_24px),
                                contentDescription = "Edit brushes"
                            )
                        }
                    }
                )
            }
        ) {
            BrushPicker(
                listViewModel = brushListViewModel,
                selected = selectedBrush,
                padding = PaddingValues(16.dp),
                compact = compact,
                onBrushSelect = {
                    onBrushSelect(it)
                    showBrushPicker = false
                }
            )
        }
    }
}