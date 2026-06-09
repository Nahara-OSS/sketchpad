package io.github.naharaoss.skpd.toolbar.ui

import android.view.Surface
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.width
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.document.DocumentViewModel
import io.github.naharaoss.skpd.toolbar.Tool
import io.github.naharaoss.skpd.toolbar.Toolbar
import kotlin.math.roundToInt

private val ButtonSize = 48.dp

@Composable
fun ToolbarOverlay(
    modifier: Modifier = Modifier,
    toolbars: List<Toolbar>,
    onToolbarsChange: (List<Toolbar>) -> Unit,
    onCloseDocument: () -> Unit,
    undockedPadding: Dp,
    documentViewModel: DocumentViewModel,
    brushListViewModel: BrushListViewModel,
    windowSizeClass: WindowSizeClass
) {
    val toolbarMap = toolbars.associateBy { it.id }
    val rotation = LocalView.current.display.rotation

    @Composable
    fun ToolbarFlow(
        modifier: Modifier = Modifier,
        orientation: Toolbar.Orientation,
        content: @Composable (reversed: Boolean) -> Unit
    ) {
        val reversed = when (orientation) {
            Toolbar.Orientation.Horizontal -> rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_180
            Toolbar.Orientation.Vertical -> rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270
        }

        val orientation = when (rotation) {
            Surface.ROTATION_90, Surface.ROTATION_270 -> orientation.rotate()
            else -> orientation
        }

        when (orientation) {
            Toolbar.Orientation.Vertical -> Column(modifier) { content(reversed) }
            Toolbar.Orientation.Horizontal -> Row(modifier) { content(reversed) }
        }
    }

    ToolbarOverlayLayout(
        modifier = modifier,
        toolbars = toolbarMap.mapValues { (_, info) ->
            val primaryAxis = ButtonSize * info.tools.size
            val secondaryAxis = ButtonSize
            val contentSize = if (info.side.orientation == Toolbar.Orientation.Horizontal) DpSize(primaryAxis, secondaryAxis) else DpSize(secondaryAxis, primaryAxis)
            ToolbarInfo(info.side, info.position, info.docked, contentSize)
        },
        undockedPadding = undockedPadding
    ) { id ->
        val toolbar = toolbarMap[id]!!
        val elevation by animateDpAsState(targetValue = if (toolbar.docked) 0.dp else 2.dp)

        Surface(
            modifier = Modifier.fillMaxSize(),
            shadowElevation = elevation,
            tonalElevation = elevation,
            shape = CircleShape
        ) {
            ToolbarFlow(
                modifier = Modifier,
                orientation = toolbar.side.orientation
            ) { reversed ->
                for (i in toolbar.tools.indices) {
                    val i = if (reversed) toolbar.tools.size - 1 - i else i
                    val (id, tool) = toolbar.tools[i]

                    val context = object : Tool.ToolContext {
                        override val documentViewModel = documentViewModel
                        override val brushListViewModel = brushListViewModel
                        override val windowSizeClass = windowSizeClass

                        override fun replaceTool(tool: Tool) {
                            onToolbarsChange(toolbars.map { tb ->
                                if (tb.id != toolbar.id) tb
                                else tb.copy(tools = tb.tools.map { tl ->
                                    if (tl.id == id) tl.copy(content = tool) else tl
                                })
                            })
                        }

                        override fun closeDocument() {
                            onCloseDocument()
                        }
                    }

                    tool.ToolbarButton(context = context)
                }
            }
        }
    }
}

/**
 * Compose the layout for toolbar overlays.
 *
 * @param [modifier] Compose modifier
 * @param [toolbars] Map of toolbar positioning info
 * @param [undockedPadding] Padding for undocked toolbars
 * @param [content] Callback to compose toolbar content
 */
@Composable
fun <K> ToolbarOverlayLayout(
    modifier: Modifier = Modifier,
    toolbars: Map<K, ToolbarInfo>,
    undockedPadding: Dp,
    content: @Composable (K) -> Unit
) {
    val rotation = LocalView.current.display.rotation
    val rotatedCutouts = LocalView.current.display.cutout?.boundingRects?.map { it.toComposeDpRect() } ?: emptyList()
    val rotatedWindowSize = LocalWindowInfo.current.containerDpSize

    fun DpRect.toNaturalRect() = when (rotation) {
        Surface.ROTATION_90 -> DpRect(left = rotatedWindowSize.height - bottom, right = rotatedWindowSize.height - top, top = left, bottom = right)
        Surface.ROTATION_180 -> DpRect(left = rotatedWindowSize.width - right, right = rotatedWindowSize.width - left, top = rotatedWindowSize.height - bottom, bottom = rotatedWindowSize.height - top)
        Surface.ROTATION_270 -> DpRect(left = top, right = bottom, top = rotatedWindowSize.width - right, bottom = rotatedWindowSize.width - left)
        else -> this
    }

    fun DpRect.toRotatedRect() = when (rotation) {
        Surface.ROTATION_90 -> DpRect(left = top, right = bottom, top = rotatedWindowSize.height - right, bottom = rotatedWindowSize.height - left)
        Surface.ROTATION_180 -> DpRect(left = rotatedWindowSize.width - right, right = rotatedWindowSize.width - left, top = rotatedWindowSize.height - bottom, bottom = rotatedWindowSize.height - top)
        Surface.ROTATION_270 -> DpRect(left = rotatedWindowSize.width - bottom, right = rotatedWindowSize.width - top, top = left, bottom = right)
        else -> this
    }

    val naturalCutouts = rotatedCutouts.map { it.toNaturalRect() }
    val naturalWindowSize = when (rotation) {
        Surface.ROTATION_90, Surface.ROTATION_270 -> DpSize(width = rotatedWindowSize.height, height = rotatedWindowSize.width)
        else -> rotatedWindowSize
    }

    val toolbarPositions = calculateToolbarPositions(
        undockedPadding = undockedPadding,
        windowSize = naturalWindowSize,
        cutouts = naturalCutouts,
        toolbars = toolbars
    )

    val toolbarRects = toolbarPositions.toolbarRects
    val dockedTop by animateDpAsState(targetValue = toolbarPositions.dockedTop)
    val dockedBottom by animateDpAsState(targetValue = toolbarPositions.dockedBottom)
    val dockedLeft by animateDpAsState(targetValue = toolbarPositions.dockedLeft)
    val dockedRight by animateDpAsState(targetValue = toolbarPositions.dockedRight)

    Box(modifier) {
        Box(Modifier
            .align(
                when (rotation) {
                    Surface.ROTATION_90 -> AbsoluteAlignment.TopLeft
                    Surface.ROTATION_180 -> AbsoluteAlignment.BottomLeft
                    Surface.ROTATION_270 -> AbsoluteAlignment.TopRight
                    else -> AbsoluteAlignment.TopLeft
                }
            )
            .size(
                when (rotation) {
                    Surface.ROTATION_90, Surface.ROTATION_270 -> DpSize(
                        width = dockedTop,
                        height = rotatedWindowSize.height
                    )

                    else -> DpSize(width = rotatedWindowSize.width, height = dockedTop)
                }
            )
            .background(color = MaterialTheme.colorScheme.surface))

        Box(Modifier
            .align(
                when (rotation) {
                    Surface.ROTATION_90 -> AbsoluteAlignment.TopRight
                    Surface.ROTATION_180 -> AbsoluteAlignment.TopLeft
                    Surface.ROTATION_270 -> AbsoluteAlignment.TopLeft
                    else -> AbsoluteAlignment.BottomLeft
                }
            )
            .size(
                when (rotation) {
                    Surface.ROTATION_90, Surface.ROTATION_270 -> DpSize(
                        width = dockedBottom,
                        height = rotatedWindowSize.height
                    )

                    else -> DpSize(width = rotatedWindowSize.width, height = dockedBottom)
                }
            )
            .background(color = MaterialTheme.colorScheme.surface))

        Box(Modifier
            .align(
                when (rotation) {
                    Surface.ROTATION_90 -> AbsoluteAlignment.BottomLeft
                    Surface.ROTATION_180 -> AbsoluteAlignment.TopRight
                    Surface.ROTATION_270 -> AbsoluteAlignment.TopRight
                    else -> AbsoluteAlignment.TopLeft
                }
            )
            .size(
                when (rotation) {
                    Surface.ROTATION_90, Surface.ROTATION_270 -> DpSize(
                        width = rotatedWindowSize.width,
                        height = dockedLeft
                    )

                    else -> DpSize(width = dockedLeft, height = rotatedWindowSize.height)
                }
            )
            .background(color = MaterialTheme.colorScheme.surface))

        Box(Modifier
            .align(
                when (rotation) {
                    Surface.ROTATION_90 -> AbsoluteAlignment.TopLeft
                    Surface.ROTATION_180 -> AbsoluteAlignment.TopLeft
                    Surface.ROTATION_270 -> AbsoluteAlignment.BottomLeft
                    else -> AbsoluteAlignment.TopRight
                }
            )
            .size(
                when (rotation) {
                    Surface.ROTATION_90, Surface.ROTATION_270 -> DpSize(
                        width = rotatedWindowSize.width,
                        height = dockedRight
                    )

                    else -> DpSize(width = dockedRight, height = rotatedWindowSize.height)
                }
            )
            .background(color = MaterialTheme.colorScheme.surface))

        for ((key, rect) in toolbarRects) {
            key(key) {
                val rect = rect.toRotatedRect()
                val x by animateDpAsState(targetValue = rect.left)
                val y by animateDpAsState(targetValue = rect.top)

                Box(Modifier
                    .absoluteOffset { IntOffset(x.toPx().roundToInt(), y.toPx().roundToInt()) }
                    .size(width = rect.width, height = rect.height)) {
                    content(key)
                }
            }
        }
    }
}

@Composable
private fun android.graphics.Rect.toComposeDpRect(density: Density = LocalDensity.current): DpRect {
    return with(density) {
        DpRect(
            left = left.toDp(),
            top = top.toDp(),
            bottom = bottom.toDp(),
            right = right.toDp()
        )
    }
}

data class ToolbarInfo(
    val side: Toolbar.Side,
    val position: Toolbar.Position,
    val docked: Boolean,
    val size: DpSize
)

private data class CalculatedToolbarPositions<K>(
    val dockedTop: Dp,
    val dockedBottom: Dp,
    val dockedLeft: Dp,
    val dockedRight: Dp,
    val toolbarRects: Map<K, DpRect>
)

private fun <K> calculateToolbarPositions(
    undockedPadding: Dp,
    windowSize: DpSize,
    cutouts: List<DpRect>,
    toolbars: Map<K, ToolbarInfo>
): CalculatedToolbarPositions<K> {
    val toolbarRects = mutableMapOf<K, DpRect>()

    // Cutout filters
    val leftFilter = 0.dp..windowSize.width / 3f
    val centerFilter = windowSize.width / 3f..windowSize.width * 2f / 3f
    val rightFilter = windowSize.width * 2f / 3f..windowSize.width
    val topFilter = 0.dp..windowSize.height / 3f
    val middleFilter = windowSize.height / 3f..windowSize.height * 2f / 3f
    val bottomFilter = windowSize.height * 2f / 3f..windowSize.height

    // Cutout metrics
    val topLeft = cutouts
        .filter { leftFilter.contains((it.left + it.right) / 2f) && topFilter.contains((it.top + it.bottom) / 2f) }
        .map { DpOffset(x = it.right, y = it.bottom) }
        .reduceOrNull { acc, offset -> DpOffset(x = max(acc.x, offset.x), y = max(acc.y, offset.y)) } ?: DpOffset(0.dp, 0.dp)
    val topCenter = cutouts
        .filter { centerFilter.contains((it.left + it.right) / 2f) && topFilter.contains((it.top + it.bottom) / 2f) }
        .maxOfOrNull { it.bottom } ?: 0.dp
    val topRight = cutouts
        .filter { rightFilter.contains((it.left + it.right) / 2f) && topFilter.contains((it.top + it.bottom) / 2f) }
        .map { DpOffset(x = it.left, y = it.bottom) }
        .reduceOrNull { acc, offset -> DpOffset(x = min(acc.x, offset.x), y = max(acc.y, offset.y)) } ?: DpOffset(windowSize.width, 0.dp)
    val middleLeft = cutouts
        .filter { leftFilter.contains((it.left + it.right) / 2f) && middleFilter.contains((it.top + it.bottom) / 2f) }
        .maxOfOrNull { it.right } ?: 0.dp
    val middleRight = cutouts
        .filter { rightFilter.contains((it.left + it.right) / 2f) && middleFilter.contains((it.top + it.bottom) / 2f) }
        .minOfOrNull { it.left } ?: windowSize.width
    val bottomLeft = cutouts
        .filter { leftFilter.contains((it.left + it.right) / 2f) && bottomFilter.contains((it.top + it.bottom) / 2f) }
        .map { DpOffset(x = it.right, y = it.top) }
        .reduceOrNull { acc, offset -> DpOffset(x = max(acc.x, offset.x), y = min(acc.y, offset.y)) } ?: DpOffset(0.dp, windowSize.height)
    val bottomCenter = cutouts
        .filter { centerFilter.contains((it.left + it.right) / 2f) && bottomFilter.contains((it.top + it.bottom) / 2f) }
        .minOfOrNull { it.top } ?: windowSize.height
    val bottomRight = cutouts
        .filter { rightFilter.contains((it.left + it.right) / 2f) && bottomFilter.contains((it.top + it.bottom) / 2f) }
        .map { DpOffset(x = it.left, y = it.top) }
        .reduceOrNull { acc, offset -> DpOffset(x = min(acc.x, offset.x), y = min(acc.y, offset.y)) } ?: DpOffset(windowSize.width, windowSize.height)

    // Horizontal docks
    val cutoutTop = maxOf(topLeft.y, topCenter, topRight.y)
    val cutoutBottom = minOf(bottomLeft.y, bottomCenter, bottomRight.y)
    val dockedTop = toolbars.values
        .filter { it.docked && it.side == Toolbar.Side.Top }
        .maxOfOrNull { max(it.size.height + if (it.position == Toolbar.Position.Center) topCenter else 0.dp, cutoutTop) } ?: 0.dp
    val dockedBottom = toolbars.values
        .filter { it.docked && it.side == Toolbar.Side.Bottom }
        .maxOfOrNull { max(it.size.height + if (it.position == Toolbar.Position.Center) (windowSize.height - bottomCenter) else 0.dp, windowSize.height - cutoutBottom) } ?: 0.dp

    for ((key, info) in toolbars.filter { it.value.docked && it.value.side.orientation == Toolbar.Orientation.Horizontal }) {
        val origin = when (info.position to info.side) {
            Toolbar.Position.Start to Toolbar.Side.Top -> DpOffset(x = topLeft.x, y = dockedTop - info.size.height)
            Toolbar.Position.Center to Toolbar.Side.Top -> DpOffset(x = (windowSize.width - info.size.width) / 2f, y = dockedTop - info.size.height)
            Toolbar.Position.End to Toolbar.Side.Top -> DpOffset(x = topRight.x - info.size.width, y = dockedTop - info.size.height)
            Toolbar.Position.Start to Toolbar.Side.Bottom -> DpOffset(x = bottomLeft.x, y = windowSize.height - dockedBottom)
            Toolbar.Position.Center to Toolbar.Side.Bottom -> DpOffset(x = (windowSize.width - info.size.width) / 2f, y = windowSize.height - dockedBottom)
            Toolbar.Position.End to Toolbar.Side.Bottom -> DpOffset(x = bottomRight.x - info.size.width, y = windowSize.height - dockedBottom)
            else -> throw Exception("Unreachable")
        }

        toolbarRects[key] = DpRect(origin = origin, size = info.size)
    }

    for ((key, info) in toolbars.filter { !it.value.docked && it.value.side.orientation == Toolbar.Orientation.Horizontal }) {
        val origin = when (info.position to info.side) {
            Toolbar.Position.Start to Toolbar.Side.Top -> DpOffset(x = topLeft.x + undockedPadding, y = undockedPadding)
            Toolbar.Position.Center to Toolbar.Side.Top -> DpOffset(x = (windowSize.width - info.size.width) / 2f, y = topCenter + undockedPadding)
            Toolbar.Position.End to Toolbar.Side.Top -> DpOffset(x = topRight.x - info.size.width - undockedPadding, y = undockedPadding)
            Toolbar.Position.Start to Toolbar.Side.Bottom -> DpOffset(x = bottomLeft.x + undockedPadding, y = windowSize.height - info.size.height - undockedPadding)
            Toolbar.Position.Center to Toolbar.Side.Bottom -> DpOffset(x = (windowSize.width - info.size.width) / 2f, y = bottomCenter - info.size.height - undockedPadding)
            Toolbar.Position.End to Toolbar.Side.Bottom -> DpOffset(x = bottomRight.x - info.size.width - undockedPadding, y = windowSize.height - info.size.height - undockedPadding)
            else -> throw Exception("Unreachable")
        }

        toolbarRects[key] = DpRect(origin = origin, size = info.size)
    }

    // Vertical docks
    val leftSidebarTop = maxOf(
        toolbars.filter { it.value.side == Toolbar.Side.Top && it.value.position == Toolbar.Position.Start }.maxOfOrNull { toolbarRects[it.key]!!.bottom } ?: 0.dp,
        dockedTop,
        topLeft.y
    )
    val leftSidebarBottom = minOf(
        toolbars.filter { it.value.side == Toolbar.Side.Bottom && it.value.position == Toolbar.Position.Start }.minOfOrNull { toolbarRects[it.key]!!.top } ?: windowSize.height,
        windowSize.height - dockedBottom,
        bottomLeft.y
    )
    val rightSidebarTop = maxOf(
        toolbars.filter { it.value.side == Toolbar.Side.Top && it.value.position == Toolbar.Position.End }.maxOfOrNull { toolbarRects[it.key]!!.bottom } ?: 0.dp,
        dockedTop,
        topRight.y
    )
    val rightSidebarBottom = minOf(
        toolbars.filter { it.value.side == Toolbar.Side.Bottom && it.value.position == Toolbar.Position.End }.minOfOrNull { toolbarRects[it.key]!!.top } ?: windowSize.height,
        windowSize.height - dockedBottom,
        bottomRight.y
    )

    val cutoutLeft = maxOf(if (dockedTop == 0.dp) topLeft.x else 0.dp, middleLeft, if (dockedBottom == 0.dp) bottomLeft.x else 0.dp)
    val cutoutRight = minOf(if (dockedTop == 0.dp) topRight.x else windowSize.width, middleRight, if (dockedBottom == 0.dp) bottomRight.x else windowSize.width)
    val dockedLeft = toolbars.values
        .filter { it.docked && it.side == Toolbar.Side.Left }
        .maxOfOrNull { max(it.size.width + if (it.position == Toolbar.Position.Center) middleLeft else 0.dp, cutoutLeft) } ?: 0.dp
    val dockedRight = toolbars.values
        .filter { it.docked && it.side == Toolbar.Side.Right }
        .maxOfOrNull { max(it.size.width + if (it.position == Toolbar.Position.Center) (windowSize.width - middleRight) else 0.dp, windowSize.width - cutoutRight) } ?: 0.dp

    for ((key, rect) in toolbars.filter { it.value.docked && it.value.side.orientation == Toolbar.Orientation.Vertical }) {
        val origin = when (rect.position to rect.side) {
            Toolbar.Position.Start to Toolbar.Side.Left -> DpOffset(x = dockedLeft - rect.size.width, y = leftSidebarTop)
            Toolbar.Position.Center to Toolbar.Side.Left -> DpOffset(x = dockedLeft - rect.size.width, y = (windowSize.height - rect.size.height) / 2f)
            Toolbar.Position.End to Toolbar.Side.Left -> DpOffset(x = dockedLeft - rect.size.width, y = leftSidebarBottom - rect.size.height)
            Toolbar.Position.Start to Toolbar.Side.Right -> DpOffset(x = windowSize.width - dockedRight, y = rightSidebarTop)
            Toolbar.Position.Center to Toolbar.Side.Right -> DpOffset(x = windowSize.width - dockedRight, y = (windowSize.height - rect.size.height) / 2f)
            Toolbar.Position.End to Toolbar.Side.Right -> DpOffset(x = windowSize.width - dockedRight, y = rightSidebarBottom - rect.size.height)
            else -> throw Exception("Unreachable")
        }

        toolbarRects[key] = DpRect(origin = origin, size = rect.size)
    }

    for ((key, rect) in toolbars.filter { !it.value.docked && it.value.side.orientation == Toolbar.Orientation.Vertical }) {
        val origin = when (rect.position to rect.side) {
            Toolbar.Position.Start to Toolbar.Side.Left -> DpOffset(x = undockedPadding, y = leftSidebarTop + undockedPadding)
            Toolbar.Position.Center to Toolbar.Side.Left -> DpOffset(x = undockedPadding, y = (windowSize.height - rect.size.height) / 2f)
            Toolbar.Position.End to Toolbar.Side.Left -> DpOffset(x = undockedPadding, y = leftSidebarBottom - rect.size.height - undockedPadding)
            Toolbar.Position.Start to Toolbar.Side.Right -> DpOffset(x = windowSize.width - rect.size.width - undockedPadding, y = rightSidebarTop + undockedPadding)
            Toolbar.Position.Center to Toolbar.Side.Right -> DpOffset(x = windowSize.width - rect.size.width - undockedPadding, y = (windowSize.height - rect.size.height) / 2f)
            Toolbar.Position.End to Toolbar.Side.Right -> DpOffset(x = windowSize.width - rect.size.width - undockedPadding, y = rightSidebarBottom - rect.size.height - undockedPadding)
            else -> throw Exception("Unreachable")
        }

        toolbarRects[key] = DpRect(origin = origin, size = rect.size)
    }

    return CalculatedToolbarPositions(
        dockedTop = dockedTop,
        dockedBottom = dockedBottom,
        dockedLeft = dockedLeft,
        dockedRight = dockedRight,
        toolbarRects = toolbarRects
    )
}