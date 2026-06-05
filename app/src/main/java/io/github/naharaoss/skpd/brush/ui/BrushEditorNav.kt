package io.github.naharaoss.skpd.brush.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.github.naharaoss.skpd.resource.BrushItem
import kotlinx.serialization.Serializable

@Serializable
sealed interface BrushEditorRoute {
    @Serializable
    object BrushList : BrushEditorRoute

    @Serializable
    data class Brush(val brush: BrushItem) : BrushEditorRoute

    @Serializable
    data class Dynamic(val brush: BrushItem, val parameter: String) : BrushEditorRoute
}

val BrushListMetadata = "type" to "BrushList"
val BrushMetadata = "type" to "Brush"
val DynamicMetadata = "type" to "Dynamic"

class BrushEditorSceneStrategy(
    private val windowSizeClass: WindowSizeClass
) : SceneStrategy<BrushEditorRoute> {
    override fun SceneStrategyScope<BrushEditorRoute>.calculateScene(entries: List<NavEntry<BrushEditorRoute>>): Scene<BrushEditorRoute> {
        fun <T : Any> checkMetadataOf(entry: NavEntry<T>, pair: Pair<String, Any>): Boolean = entry.metadata[pair.first] == pair.second

        if (checkMetadataOf(entries.last(), BrushListMetadata)) {
            return SinglePaneScene(
                key = entries.last().contentKey,
                entry = entries.last(),
                previousEntries = entries.dropLast(1),
                width = 500.dp
            )
        }

        if (
            entries.size >= 2 &&
            checkMetadataOf(entries[entries.lastIndex - 1], BrushMetadata) &&
            checkMetadataOf(entries[entries.lastIndex], DynamicMetadata) &&
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        ) {
            return DoublePaneScene(
                key = entries[entries.lastIndex - 1].contentKey to entries[entries.lastIndex].contentKey,
                previousEntries = entries.dropLast(1),
                first = entries[entries.lastIndex - 1],
                second = entries[entries.lastIndex]
            )
        }

        return SinglePaneScene(
            key = entries.last().contentKey,
            entry = entries.last(),
            previousEntries = entries.dropLast(1),
            width = 400.dp
        )
    }
}

private class SinglePaneScene(
    override val key: Any,
    override val previousEntries: List<NavEntry<BrushEditorRoute>>,
    val entry: NavEntry<BrushEditorRoute>,
    val width: Dp
) : Scene<BrushEditorRoute> {
    override val entries: List<NavEntry<BrushEditorRoute>> = listOf(entry)
    override val content: @Composable (() -> Unit) = {
        Surface(
            modifier = Modifier
                .width(width)
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .consumeWindowInsets(TopAppBarDefaults.windowInsets.only(WindowInsetsSides.End))
        ) {
            entry.Content()
        }
    }
}

private class DoublePaneScene(
    override val key: Any,
    override val previousEntries: List<NavEntry<BrushEditorRoute>>,
    val first: NavEntry<BrushEditorRoute>,
    val second: NavEntry<BrushEditorRoute>
) : Scene<BrushEditorRoute> {
    override val entries: List<NavEntry<BrushEditorRoute>> = listOf(first, second)
    override val content: @Composable (() -> Unit) = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                modifier = Modifier
                    .width(400.dp)
                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .consumeWindowInsets(TopAppBarDefaults.windowInsets.only(WindowInsetsSides.End))
            ) {
                first.Content()
            }

            Surface(
                modifier = Modifier
                    .width(400.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .consumeWindowInsets(TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End))
            ) {
                second.Content()
            }
        }
    }
}