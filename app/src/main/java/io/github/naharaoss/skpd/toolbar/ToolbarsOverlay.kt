package io.github.naharaoss.skpd.toolbar

import android.view.DisplayCutout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.width
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.settings.Toolbar
import io.github.naharaoss.skpd.utils.moveBy

private val ToolbarIconSize = 48.dp
private val ToolbarDockedPadding = 4.dp
private val ToolbarFloatingPadding = 12.dp

data class Computed(
    val computedToolbars: Map<Toolbar, ComputedToolbar>,
    val windowSize: DpSize
) {
    fun sideSize(side: Toolbar.Side, mustBeDocked: Boolean): Dp {
        val toolbars = computedToolbars.values.filter { it.toolbar.side == side && (!mustBeDocked || it.toolbar.docked) }

        return when (side) {
            Toolbar.Side.Top -> toolbars.maxOfOrNull { it.finalRect.bottom } ?: 0.dp
            Toolbar.Side.Bottom -> toolbars.maxOfOrNull { windowSize.height - it.finalRect.top } ?: 0.dp
            Toolbar.Side.Start -> toolbars.maxOfOrNull { it.finalRect.right } ?: 0.dp
            Toolbar.Side.End -> toolbars.maxOfOrNull { windowSize.width - it.finalRect.left } ?: 0.dp
        }
    }
}

data class ComputedToolbar(
    val toolbar: Toolbar,
    val originalRect: DpRect,
    val offset: DpOffset
) {
    val finalRect get() = originalRect.moveBy(offset)
}

@Composable
fun compute(
    toolbars: List<Toolbar>,
    displayCutout: DisplayCutout?
): Computed {
    val computedToolbars = mutableMapOf<Toolbar, ComputedToolbar>()
    val windowSize = LocalWindowInfo.current.containerDpSize
    val result = Computed(computedToolbars, windowSize)
    val cutoutRects = displayCutout?.boundingRects?.map { it.toComposeRect() } ?: emptyList()

    // Collect toolbar rectangles
    for (toolbar in toolbars.sortedBy { it.side }) {
        val toolbarPadding = if (toolbar.docked) ToolbarDockedPadding else ToolbarFloatingPadding
        val width = when (toolbar.side.orientation) {
            Toolbar.Orientation.Row -> toolbar.tools.size * ToolbarIconSize + toolbarPadding * 2
            Toolbar.Orientation.Column -> ToolbarIconSize + toolbarPadding * 2
        }
        val height = when (toolbar.side.orientation) {
            Toolbar.Orientation.Row -> ToolbarIconSize + toolbarPadding * 2
            Toolbar.Orientation.Column -> toolbar.tools.size * ToolbarIconSize + toolbarPadding * 2
        }
        val x = when (toolbar.side) {
            Toolbar.Side.Top, Toolbar.Side.Bottom -> when (toolbar.align) {
                Toolbar.Align.Start -> 0.dp
                Toolbar.Align.Middle -> (windowSize.width - width) / 2
                Toolbar.Align.End -> windowSize.width - width
            }
            Toolbar.Side.Start -> 0.dp
            Toolbar.Side.End -> windowSize.width - width
        }
        val y = when (toolbar.side) {
            Toolbar.Side.Start, Toolbar.Side.End -> when (toolbar.align) {
                Toolbar.Align.Start -> 0.dp
                Toolbar.Align.Middle -> (windowSize.height - height) / 2
                Toolbar.Align.End -> windowSize.height - height
            }
            Toolbar.Side.Top -> 0.dp
            Toolbar.Side.Bottom -> windowSize.height - height
        }

        computedToolbars[toolbar] = ComputedToolbar(
            toolbar = toolbar,
            originalRect = DpRect(origin = DpOffset(x, y), size = DpSize(width, height)),
            offset = DpOffset(0.dp, 0.dp)
        )
    }

    val centralToolbars = mutableListOf<Toolbar>()
    val sideToolbars = mutableListOf<Toolbar>()
    val unfilteredToolbars = mutableListOf<Toolbar>()

    for (toolbar in toolbars) {
        val list = when {
            toolbar.align == Toolbar.Align.Middle -> centralToolbars
            toolbar.side.orientation == Toolbar.Orientation.Column -> sideToolbars
            else -> unfilteredToolbars
        }

        list.add(toolbar)
    }

    // For toolbars aligned to center
    centralToolbars.forEach { toolbar ->
        var computed = computedToolbars[toolbar]!!

        for (cutoutRect in cutoutRects) {
            val computedRect = computed.finalRect
            val computedRectPx = with(LocalDensity.current) {
                Rect(
                    offset = Offset(computedRect.left.toPx(), computedRect.top.toPx()),
                    size = Size(computedRect.width.toPx(), computedRect.height.toPx())
                )
            }

            if (!cutoutRect.overlaps(computedRectPx)) continue

            with(LocalDensity.current) {
                computed = when (toolbar.side) {
                    Toolbar.Side.Top -> computed.copy(offset = computed.offset.copy(y = max(cutoutRect.bottom.toDp(), computed.offset.y)))
                    Toolbar.Side.Bottom -> computed.copy(offset = computed.offset.copy(y = min(-(windowSize.height - cutoutRect.top.toDp()), computed.offset.y)))
                    Toolbar.Side.Start -> computed.copy(offset = computed.offset.copy(x = max(cutoutRect.right.toDp(), computed.offset.x)))
                    Toolbar.Side.End -> computed.copy(offset = computed.offset.copy(x = min(-(windowSize.height - cutoutRect.left.toDp()), computed.offset.x)))
                }
            }
        }

        if (toolbar.docked) {
            toolbars.filter { it != toolbar && it.docked && it.side == toolbar.side }.forEach {
                computedToolbars[it] = computedToolbars[it]!!.copy(offset = when (it.side) {
                    Toolbar.Side.Top, Toolbar.Side.Bottom -> computedToolbars[it]!!.offset.copy(y = computed.offset.y)
                    Toolbar.Side.Start, Toolbar.Side.End -> computedToolbars[it]!!.offset.copy(x = computed.offset.x)
                })
            }
        }

        computedToolbars[toolbar] = computed
    }

    // Side toolbars is now being squeezed by top and bottom docs, so we have to recalculate them
    sideToolbars.forEach { toolbar ->
        computedToolbars[toolbar] = when (toolbar.align) {
            Toolbar.Align.Start -> computedToolbars[toolbar]!!.copy(offset = computedToolbars[toolbar]!!.offset.copy(y = result.sideSize(Toolbar.Side.Top, false)))
            Toolbar.Align.End -> computedToolbars[toolbar]!!.copy(offset = computedToolbars[toolbar]!!.offset.copy(y = -result.sideSize(Toolbar.Side.Bottom, false)))
            else -> computedToolbars[toolbar]!!
        }
    }

    // TODO: Corners
    // TODO: More testing
    return result
}

/**
 * Overlay for toolbars.
 *
 * This composable is meant to be overlaid on top of another element, such as
 * [android.view.SurfaceView] or a fullscreen composable:
 *
 * ```kotlin
 * Box(Modifier.fillMaxSize()) {
 *     DrawingCanvas(
 *         modifier = Modifier.fillMaxSize(),
 *         // ...
 *     )
 *
 *     ToolbarsOverlay(
 *         modifier = Modifier.fillMaxSize(),
 *         // ...
 *     )
 * }
 * ```
 */
@Composable
fun ToolbarsOverlay(
    modifier: Modifier = Modifier,
    toolbars: List<Toolbar>,
    displayCutout: DisplayCutout?
) {
    val computed = compute(toolbars, displayCutout)
    val topDockSize by animateDpAsState(computed.sideSize(Toolbar.Side.Top, true), tween(easing = FastOutSlowInEasing))
    val bottomDockSize by animateDpAsState(computed.sideSize(Toolbar.Side.Bottom, true), tween(easing = FastOutSlowInEasing))
    val startDockSize by animateDpAsState(computed.sideSize(Toolbar.Side.Start, true), tween(easing = FastOutSlowInEasing))
    val endDockSize by animateDpAsState(computed.sideSize(Toolbar.Side.End, true), tween(easing = FastOutSlowInEasing))

    Box(modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(topDockSize)
                .background(MaterialTheme.colorScheme.surface)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(bottomDockSize)
                .background(MaterialTheme.colorScheme.surface)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxHeight()
                .width(startDockSize)
                .background(MaterialTheme.colorScheme.surface)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(endDockSize)
                .background(MaterialTheme.colorScheme.surface)
        )

        for ((toolbar, computed) in computed.computedToolbars) {
            val rect = computed.finalRect
            val left by animateDpAsState(rect.left, tween(easing = FastOutSlowInEasing))
            val top by animateDpAsState(rect.top, tween(easing = FastOutSlowInEasing))

            ToolbarComponent(
                modifier = Modifier.offset { IntOffset(left.toPx().toInt(), top.toPx().toInt()) },
                docked = toolbar.docked
            ) {
                when (toolbar.side.orientation) {
                    Toolbar.Orientation.Row -> Row {
                        for (tool in toolbar.tools) {
                            IconButton({}) {
                                Icon(
                                    painter = tool.icon,
                                    contentDescription = tool.name
                                )
                            }
                        }
                    }
                    Toolbar.Orientation.Column -> Column {
                        for (tool in toolbar.tools) {
                            IconButton({}) {
                                Icon(
                                    painter = tool.icon,
                                    contentDescription = tool.name
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolbarComponent(
    modifier: Modifier = Modifier,
    docked: Boolean,
    content: @Composable () -> Unit
) {
    val padding by animateDpAsState(if (docked) 0.dp else ToolbarFloatingPadding - ToolbarDockedPadding, tween(easing = FastOutSlowInEasing))
    val elevation by animateDpAsState(if (docked) 0.dp else 2.dp, tween(easing = LinearEasing))

    Surface(
        modifier = modifier.padding(padding),
        tonalElevation = elevation,
        shadowElevation = elevation,
        shape = CircleShape
    ) {
        Box(Modifier.padding(ToolbarDockedPadding)) {
            content()
        }
    }
}