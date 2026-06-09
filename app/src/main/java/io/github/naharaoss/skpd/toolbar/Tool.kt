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
        context: ToolContext
    )

    interface ToolContext {
        val documentViewModel: DocumentViewModel
        val brushListViewModel: BrushListViewModel
        val windowSizeClass: WindowSizeClass
        fun replaceTool(tool: Tool)
        fun closeDocument()
    }

    companion object {
        val DefaultTools = listOf(
            Exit,
            ResetTransform,
            Layers,
            Brush(null)
        )
    }

    @Serializable
    @SerialName("exit")
    object Exit : Tool {
        @Composable
        override fun ToolbarButton(modifier: Modifier, context: ToolContext) {
            IconButton({ context.closeDocument() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    contentDescription = "Go back"
                )
            }
        }
    }

    @Serializable
    @SerialName("reset-transform")
    object ResetTransform : Tool {
        @Composable
        override fun ToolbarButton(modifier: Modifier, context: ToolContext) {
            val canvasTransform by context.documentViewModel.canvasTransform.collectAsState()

            FilledIconToggleButton(
                checked = canvasTransform != Matrix4.Identity,
                onCheckedChange = { context.documentViewModel.setCanvasTransform(Matrix4.Identity) }
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
        override fun ToolbarButton(modifier: Modifier, context: ToolContext) {
            LayersToolbarButton(
                modifier = modifier,
                documentViewModel = context.documentViewModel
            )
        }
    }

    @Serializable
    @SerialName("brush")
    data class Brush(val brushId: Long?) : Tool {
        @Composable
        override fun ToolbarButton(modifier: Modifier, context: ToolContext) {
            var toolBrush: BrushItem? by remember { mutableStateOf(null) }

            BrushToolbarButton(
                modifier = modifier,
                brush = toolBrush,
                compact = context.windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded,
                documentViewModel = context.documentViewModel,
                brushListViewModel = context.brushListViewModel,
                onBrushSelect = {
                    context.documentViewModel.setBrush(it)
                    context.replaceTool(copy(brushId = it.id))
                }
            )

            LaunchedEffect(brushId) {
                val brush = brushId?.let { id -> context.brushListViewModel.getBrushById(id) }
                toolBrush = brush
            }
        }
    }
}