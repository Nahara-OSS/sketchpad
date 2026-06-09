package io.github.naharaoss.skpd.toolbar

import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.document.DocumentViewModel
import io.github.naharaoss.skpd.resource.BrushItem
import io.github.naharaoss.skpd.toolbar.ui.BrushToolbarButton
import io.github.naharaoss.skpd.toolbar.ui.LayersToolbarButton
import io.github.naharaoss.skpd.utils.Matrix4
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Tool {
    @Composable
    fun ToolbarButton(
        modifier: Modifier = Modifier,
        windowSizeClass: WindowSizeClass,
        onReplaceTool: (Tool) -> Unit,
        onCloseDocument: () -> Unit
    )

    @Serializable
    @SerialName("exit")
    object Exit : Tool {
        @Composable
        override fun ToolbarButton(
            modifier: Modifier,
            windowSizeClass: WindowSizeClass,
            onReplaceTool: (Tool) -> Unit,
            onCloseDocument: () -> Unit
        ) {
            IconButton(onCloseDocument) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    contentDescription = "Go back"
                )
            }
        }
    }

    @Serializable
    @SerialName("undo")
    object Undo : Tool {
        @Composable
        override fun ToolbarButton(
            modifier: Modifier,
            windowSizeClass: WindowSizeClass,
            onReplaceTool: (Tool) -> Unit,
            onCloseDocument: () -> Unit
        ) {
            IconButton({}) {
                Icon(
                    painter = painterResource(R.drawable.undo_24px),
                    contentDescription = "Undo"
                )
            }
        }
    }

    @Serializable
    @SerialName("redo")
    object Redo : Tool {
        @Composable
        override fun ToolbarButton(
            modifier: Modifier,
            windowSizeClass: WindowSizeClass,
            onReplaceTool: (Tool) -> Unit,
            onCloseDocument: () -> Unit
        ) {
            IconButton({}) {
                Icon(
                    painter = painterResource(R.drawable.redo_24px),
                    contentDescription = "Redo"
                )
            }
        }
    }

    @Serializable
    @SerialName("color-picker")
    object ColorPicker : Tool {
        @Composable
        override fun ToolbarButton(
            modifier: Modifier,
            windowSizeClass: WindowSizeClass,
            onReplaceTool: (Tool) -> Unit,
            onCloseDocument: () -> Unit
        ) {
            FilledIconToggleButton(
                checked = false,
                onCheckedChange = {}
            ) {
                Icon(
                    painter = painterResource(R.drawable.palette_24px),
                    contentDescription = "Color picker"
                )
            }
        }
    }

    @Serializable
    @SerialName("color-sampler")
    object ColorSampler : Tool {
        @Composable
        override fun ToolbarButton(
            modifier: Modifier,
            windowSizeClass: WindowSizeClass,
            onReplaceTool: (Tool) -> Unit,
            onCloseDocument: () -> Unit
        ) {
            FilledIconToggleButton(
                checked = false,
                onCheckedChange = {}
            ) {
                Icon(
                    painter = painterResource(R.drawable.colorize_24px),
                    contentDescription = "Color picker"
                )
            }
        }
    }

    @Serializable
    @SerialName("reset-transform")
    object ResetTransform : Tool {
        @Composable
        override fun ToolbarButton(
            modifier: Modifier,
            windowSizeClass: WindowSizeClass,
            onReplaceTool: (Tool) -> Unit,
            onCloseDocument: () -> Unit
        ) {
            val documentViewModel: DocumentViewModel = hiltViewModel()
            val canvasTransform by documentViewModel.canvasTransform.collectAsState()

            FilledIconToggleButton(
                checked = canvasTransform != Matrix4.Identity,
                onCheckedChange = { documentViewModel.setCanvasTransform(Matrix4.Identity) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.rotate_left_24px),
                    contentDescription = "Reset transform"
                )
            }
        }
    }

    @Serializable
    @SerialName("layers")
    object Layers : Tool {
        @Composable
        override fun ToolbarButton(
            modifier: Modifier,
            windowSizeClass: WindowSizeClass,
            onReplaceTool: (Tool) -> Unit,
            onCloseDocument: () -> Unit
        ) {
            val documentViewModel: DocumentViewModel = hiltViewModel()

            LayersToolbarButton(
                modifier = modifier,
                documentViewModel = documentViewModel
            )
        }
    }

    @Serializable
    @SerialName("menu")
    object Menu : Tool {
        @Composable
        override fun ToolbarButton(
            modifier: Modifier,
            windowSizeClass: WindowSizeClass,
            onReplaceTool: (Tool) -> Unit,
            onCloseDocument: () -> Unit
        ) {
            FilledIconToggleButton(
                checked = false,
                onCheckedChange = {}
            ) {
                Icon(
                    painter = painterResource(R.drawable.menu_24px),
                    contentDescription = "Menu"
                )
            }
        }
    }

    @Serializable
    @SerialName("brush")
    data class Brush(val brushId: Long?) : Tool {
        @Composable
        override fun ToolbarButton(
            modifier: Modifier,
            windowSizeClass: WindowSizeClass,
            onReplaceTool: (Tool) -> Unit,
            onCloseDocument: () -> Unit
        ) {
            val documentViewModel: DocumentViewModel = hiltViewModel()
            val brushListViewModel: BrushListViewModel = hiltViewModel()
            var toolBrush: BrushItem? by remember { mutableStateOf(null) }

            BrushToolbarButton(
                modifier = modifier,
                brush = toolBrush,
                compact = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded,
                documentViewModel = documentViewModel,
                brushListViewModel = brushListViewModel,
                onBrushSelect = {
                    documentViewModel.setBrush(it)
                    onReplaceTool(copy(brushId = it.id))
                }
            )

            LaunchedEffect(brushId) {
                val brush = brushId?.let { id -> brushListViewModel.getBrushById(id) }
                toolBrush = brush
            }
        }
    }
}