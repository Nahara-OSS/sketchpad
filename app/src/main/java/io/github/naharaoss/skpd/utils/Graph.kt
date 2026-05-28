package io.github.naharaoss.skpd.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.utils.EditingGraph.Handle
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Bezier-backed mapping graph.
 */
@Serializable
data class Graph(val markers: List<Marker> = emptyList()) : (Float) -> Float {
    fun findMarkerPair(x: Float): Pair<Marker, Marker> {
        val search = markers.binarySearchBy(x) { it.target.x }
        if (search >= 0) return markers[search] to markers[search]

        val insertAt = -(search + 1)
        val a = if (insertAt > 0) markers[insertAt - 1] else Marker(target = ControlPoint(x = 0f, y = 0f))
        val b = if (insertAt <= markers.lastIndex) markers[insertAt] else Marker(target = ControlPoint(x = 1f, y = 1f))
        return a to b
    }

    override fun invoke(x: Float): Float {
        val pair = findMarkerPair(x)
        if (pair.first == pair.second) return pair.first.target.y

        var tS = 0f // t start
        var tE = 1f // t end

        while (abs(tE - tS) > 1e-6f) {
            val midpoint = (tS + tE) / 2f
            val bezierPoint = pair.bezier(midpoint)

            when {
                bezierPoint.x < x -> tS = midpoint
                bezierPoint.x > x -> tE = midpoint
                else -> return bezierPoint.y
            }
        }

        return pair.bezier((tS + tE) / 2f).y
    }

    fun toReadjusted(): Readjusted {
        val markers = ArrayList(
            markers
                .mapIndexed { index, marker -> marker to index }
                .sortedBy { it.first.target.x }
        )

        for (i in markers.indices) {
            val minX = if (i > 0) markers[i - 1].first.target.x else 0f
            val midX = markers[i].first.target.x
            val maxX = if (i < markers.lastIndex) markers[i + 1].first.target.x else 1f

            markers[i] = markers[i].copy(first = markers[i].first.copy(
                lcp = markers[i].first.lcp.copy(x = min(max(markers[i].first.lcp.x, minX), midX), y = min(max(markers[i].first.lcp.y, 0f), 1f)),
                rcp = markers[i].first.rcp.copy(x = min(max(markers[i].first.rcp.x, midX), maxX), y = min(max(markers[i].first.rcp.y, 0f), 1f))
            ))
        }

        return Readjusted(
            graph = Graph(markers.map { it.first }),
            newIndices = markers
                .mapIndexed { index, pair -> pair.second to index }
                .sortedBy { it.first }
                .map { it.second }
        )
    }

    /**
     * Plot the graph to byte buffer.
     *
     * Commonly used together with [android.opengl.GLES30.glTexImage2D] to provide lookup table to
     * shaders. The internal format should be [android.opengl.GLES30.GL_RED] and the data type
     * should be [android.opengl.GLES30.GL_UNSIGNED_BYTE].
     *
     * @param [buffer] The byte buffer to receive the plot
     * @param [count] Number of bins
     */
    fun plotToBuffer(buffer: ByteBuffer, count: Int) {
        val max = count - 1
        for (i in 0..max) buffer.put((this(i.toFloat() / max) * 255f).toInt().toByte())
    }

    @Serializable
    data class Marker(val target: ControlPoint, val lcp: ControlPoint = target, val rcp: ControlPoint = target)

    @Serializable
    data class ControlPoint(val x: Float, val y: Float) {
        companion object {
            val HandleSize = 32.dp
        }

        fun toCenterInRect(rect: Rect) = Offset(
            x = lerp(rect.bottomLeft.x, rect.topRight.x, x),
            y = lerp(rect.bottomLeft.y, rect.topRight.y, y)
        )

        fun Density.toExtendedCenterInRect(rect: Rect, target: ControlPoint, overlapsAltX: Float): Offset {
            val targetCenter = target.toCenterInRect(rect)

            return targetCenter + when {
                this@ControlPoint == target -> Offset(x = overlapsAltX, y = 0f) * HandleSize.toPx() * 2f
                else -> (toCenterInRect(rect) - targetCenter).normalized * HandleSize.toPx() * 2f
            }
        }
    }

    data class Readjusted(val graph: Graph, val newIndices: List<Int>)
}

private fun lerp(a: Graph.ControlPoint, b: Graph.ControlPoint, fraction: Float) = Graph.ControlPoint(
    x = a.x * (1f - fraction) + b.x * fraction,
    y = a.y * (1f - fraction) + b.y * fraction
)

private fun Pair<Graph.Marker, Graph.Marker>.bezier(t: Float): Graph.ControlPoint {
    val (a, b) = this
    val cp01 = lerp(a.target, a.rcp, t)
    val cp12 = lerp(a.rcp, b.lcp, t)
    val cp23 = lerp(b.lcp, b.target, t)
    val cp012 = lerp(cp01, cp12, t)
    val cp123 = lerp(cp12, cp23, t)
    return lerp(cp012, cp123, t)
}

private data class EditingGraph(val graph: Graph, val index: Int, val handle: Handle) {
    fun withMovedHandle(dx: Float, dy: Float): EditingGraph {
        val (graph, newIndices) = Graph(markers = graph.markers.replaceAt(index, when (handle) {
            Handle.Target -> graph.markers[index].copy(
                target = graph.markers[index].target.copy(x = graph.markers[index].target.x + dx, y = graph.markers[index].target.y + dy),
                lcp = graph.markers[index].lcp.copy(x = graph.markers[index].lcp.x + dx, y = graph.markers[index].lcp.y + dy),
                rcp = graph.markers[index].rcp.copy(x = graph.markers[index].rcp.x + dx, y = graph.markers[index].rcp.y + dy)
            )
            Handle.Left -> graph.markers[index].copy(lcp = graph.markers[index].lcp.copy(x = graph.markers[index].lcp.x + dx, y = graph.markers[index].lcp.y + dy))
            Handle.Right -> graph.markers[index].copy(rcp = graph.markers[index].rcp.copy(x = graph.markers[index].rcp.x + dx, y = graph.markers[index].rcp.y + dy))
        })).toReadjusted()

        return EditingGraph(
            graph = graph,
            index = newIndices[index],
            handle = handle
        )
    }

    enum class Handle { Target, Left, Right }
}

/**
 * A graph editor for [Graph].
 *
 * - To add new point, tap on the graph;
 * - To edit the point, select it first, then drag the handles;
 * - To delete the point, drag the main control point to somewhere outside the box.
 *
 * In some cases where the primary handle overlaps the secondary handles, the graph editor will
 * display extended drag handles (appears as outlined circles).
 *
 * @param [modifier] Compose's [Modifier]
 * @param [enabled] Whether to allow user to modify the curve
 * @param [xAxisLabel] Label for X axis
 * @param [yAxisLabel] Label for Y axis
 * @param [graph] The graph data
 * @param [onGraphChange] Callback to be called when the graph is changed
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GraphEditor(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    xAxisLabel: @Composable () -> Unit = {},
    yAxisLabel: @Composable () -> Unit = {},
    graph: Graph,
    onGraphChange: (Graph) -> Unit
) {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val deleteColor = MaterialTheme.colorScheme.error
    val targetHandleColor = MaterialTheme.colorScheme.primary
    val controlPointColor = MaterialTheme.colorScheme.secondary
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var deferredSelectIndex by remember { mutableIntStateOf(-1) }
    var editingGraph: EditingGraph? by remember { mutableStateOf(null) }

    // Changing selectedIndex in pointerInput() would reset the coroutine,
    // which is why we have to defer it to next composition
    LaunchedEffect(graph) {
        when {
            deferredSelectIndex >= 0 -> {
                selectedIndex = deferredSelectIndex
                deferredSelectIndex = -1
            }
            selectedIndex > graph.markers.lastIndex -> selectedIndex = graph.markers.lastIndex
            selectedIndex < 0 && graph.markers.isNotEmpty() -> selectedIndex = 0
        }
    }

    Box(modifier) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            Row(
                modifier = Modifier.align(Alignment.BottomStart).offset(x = 36.dp, y = (-30).dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                xAxisLabel()
                Icon(painterResource(R.drawable.arrow_right_alt_24px), "Input axis")
            }

            Column(
                modifier = Modifier.align(Alignment.TopStart).offset(x = 36.dp, y = 30.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                yAxisLabel()
                Icon(painterResource(R.drawable.arrow_upward_alt_24px), "Output axis")
            }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(graph, selectedIndex, enabled) {
                    if (!enabled) return@pointerInput

                    val gridRect = Rect(
                        offset = Offset(x = (Graph.ControlPoint.HandleSize / 2 + 4.dp).toPx(), y = (Graph.ControlPoint.HandleSize / 2 + 4.dp).toPx()),
                        size = Size(width = size.width - (Graph.ControlPoint.HandleSize + 8.dp).toPx(), height = size.height - (Graph.ControlPoint.HandleSize + 8.dp).toPx())
                    )

                    fun Graph.ControlPoint.hitTest(clicked: Offset): Boolean {
                        val radius = Graph.ControlPoint.HandleSize.toPx() / 2
                        return (clicked - toCenterInRect(gridRect)).getDistance() <= radius
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown()

                        // Dragging handles of selected marker
                        if (selectedIndex >= 0) {
                            val selected = graph.markers[selectedIndex]
                            val maxExtendedDistance = (Graph.ControlPoint.HandleSize * 1.2f).toPx()
                            val handleRadiusPx = Graph.ControlPoint.HandleSize.toPx()
                            val targetCenter = graph.markers[selectedIndex].target.toCenterInRect(gridRect)
                            val extendedLcp = with(graph.markers[selectedIndex].lcp) { toExtendedCenterInRect(gridRect, graph.markers[selectedIndex].target, -1f) }
                            val extendedRcp = with(graph.markers[selectedIndex].rcp) { toExtendedCenterInRect(gridRect, graph.markers[selectedIndex].target, +1f) }
                            val lcpLength = (graph.markers[selectedIndex].lcp.toCenterInRect(gridRect) - targetCenter).getDistance()
                            val rcpLength = (graph.markers[selectedIndex].rcp.toCenterInRect(gridRect) - targetCenter).getDistance()

                            editingGraph = when {
                                selected.target.hitTest(down.position) -> EditingGraph(graph = graph, index = selectedIndex, handle = Handle.Target)
                                selected.lcp.hitTest(down.position) -> EditingGraph(graph = graph, index = selectedIndex, handle = Handle.Left)
                                selected.rcp.hitTest(down.position) -> EditingGraph(graph = graph, index = selectedIndex, handle = Handle.Right)
                                lcpLength < maxExtendedDistance && (extendedLcp - down.position).getDistance() <= handleRadiusPx -> EditingGraph(graph = graph, index = selectedIndex, handle = Handle.Left)
                                rcpLength < maxExtendedDistance && (extendedRcp - down.position).getDistance() <= handleRadiusPx -> EditingGraph(graph = graph, index = selectedIndex, handle = Handle.Right)
                                else -> null
                            }

                            if (editingGraph != null) {
                                drag(down.id) { change ->
                                    editingGraph = editingGraph?.withMovedHandle(
                                        dx = change.positionChange().x / (gridRect.topRight.x - gridRect.bottomLeft.x),
                                        dy = change.positionChange().y / (gridRect.topRight.y - gridRect.bottomLeft.y)
                                    )
                                    change.consume()
                                }

                                editingGraph?.also { editingGraph ->
                                    val markers = editingGraph.graph.markers
                                        .filter { it.target.x in -0.1f..1.1f && it.target.y in -0.1f..1.1f }
                                        .map {
                                            val x = if (it.target.x < 0f) 0f else if (it.target.x > 1f) 1f else it.target.x
                                            val y = if (it.target.y < 0f) 0f else if (it.target.y > 1f) 1f else it.target.y
                                            it.copy(target = it.target.copy(x = x, y = y))
                                        }
                                    onGraphChange(editingGraph.graph.copy(markers = markers))
                                }

                                selectedIndex = editingGraph?.index ?: selectedIndex
                                editingGraph = null
                                return@awaitEachGesture
                            }
                        }

                        // Select different marker
                        for (i in 0..graph.markers.lastIndex) {
                            if (i == selectedIndex) continue

                            if (graph.markers[i].target.hitTest(down.position)) {
                                selectedIndex = i
                                editingGraph = null
                                return@awaitEachGesture
                            }
                        }

                        // Create new marker
                        val x = min(max((down.position.x - gridRect.left) / (gridRect.right - gridRect.left), 0f), 1f)
                        val y = 1f - min(max((down.position.y - gridRect.top) / (gridRect.bottom - gridRect.top), 0f), 1f)
                        val (newGraph, indices) = Graph(graph.markers + Graph.Marker(target = Graph.ControlPoint(x, y))).toReadjusted()
                        deferredSelectIndex = indices.last()
                        onGraphChange(newGraph)
                    }
                }
        ) {
            val gridRect = Rect(
                offset = Offset(x = (Graph.ControlPoint.HandleSize / 2 + 4.dp).toPx(), y = (Graph.ControlPoint.HandleSize / 2 + 4.dp).toPx()),
                size = Size(width = size.width - (Graph.ControlPoint.HandleSize + 8.dp).toPx(), height = size.height - (Graph.ControlPoint.HandleSize + 8.dp).toPx())
            )

            drawRect(
                color = gridColor,
                topLeft = gridRect.topLeft,
                size = gridRect.size,
                style = Stroke(width = 4.dp.toPx())
            )

            for (i in 1..<4) {
                drawLine(
                    color = gridColor,
                    start = gridRect.topLeft + Offset(x = 0f, y = gridRect.height * i / 4),
                    end = gridRect.topRight + Offset(x = 0f, y = gridRect.height * i / 4),
                    strokeWidth = 1.dp.toPx()
                )

                drawLine(
                    color = gridColor,
                    start = gridRect.topLeft + Offset(x = gridRect.width * i / 4, y = 0f),
                    end = gridRect.bottomLeft + Offset(x = gridRect.width * i / 4, y = 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val editingGraph = editingGraph
            val selectedIndex = editingGraph?.index ?: selectedIndex
            val path = Path()
            var cp1 = Graph.ControlPoint(x = 0f, y = graph(0f))

            path.moveTo(
                gridRect.bottomLeft.x,
                lerp(gridRect.bottomLeft.y, gridRect.topRight.y, graph(0f))
            )

            for (i in 0..graph.markers.lastIndex) {
                val marker = if (editingGraph != null) editingGraph.graph.markers[i] else graph.markers[i]

                path.cubicTo(
                    lerp(gridRect.bottomLeft.x, gridRect.topRight.x, min(max(cp1.x, 0f), 1f)),
                    lerp(gridRect.bottomLeft.y, gridRect.topRight.y, min(max(cp1.y, 0f), 1f)),
                    lerp(gridRect.bottomLeft.x, gridRect.topRight.x, min(max(marker.lcp.x, 0f), 1f)),
                    lerp(gridRect.bottomLeft.y, gridRect.topRight.y, min(max(marker.lcp.y, 0f), 1f)),
                    lerp(gridRect.bottomLeft.x, gridRect.topRight.x, min(max(marker.target.x, 0f), 1f)),
                    lerp(gridRect.bottomLeft.y, gridRect.topRight.y, min(max(marker.target.y, 0f), 1f))
                )

                cp1 = marker.rcp
            }

            path.cubicTo(
                lerp(gridRect.bottomLeft.x, gridRect.topRight.x, min(max(cp1.x, 0f), 1f)),
                lerp(gridRect.bottomLeft.y, gridRect.topRight.y, min(max(cp1.y, 0f), 1f)),
                gridRect.topRight.x,
                lerp(gridRect.bottomLeft.y, gridRect.topRight.y, graph(1f)),
                gridRect.topRight.x,
                lerp(gridRect.bottomLeft.y, gridRect.topRight.y, graph(1f))
            )

            drawPath(
                color = lineColor,
                path = path,
                style = Stroke(width = 4.dp.toPx())
            )

            if (enabled) for (i in 0..graph.markers.lastIndex) {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx()))
                val selected = selectedIndex == i
                val marker = if (editingGraph != null) editingGraph.graph.markers[i] else graph.markers[i]
                val deleting = marker.target.x !in -0.1f..1.1f || marker.target.y !in -0.1f..1.1f

                val target = Offset(
                    x = lerp(gridRect.bottomLeft.x, gridRect.topRight.x, min(max(marker.target.x, 0f), 1f)),
                    y = lerp(gridRect.bottomLeft.y, gridRect.topRight.y, min(max(marker.target.y, 0f), 1f))
                )

                drawLine(
                    color = lineColor,
                    start = Offset(x = target.x, y = gridRect.top),
                    end = Offset(x = target.x, y = gridRect.bottom),
                    strokeWidth = 4.dp.toPx(),
                    pathEffect = pathEffect
                )

                if (selected && !deleting) {
                    val lcp = Offset(
                        x = lerp(gridRect.bottomLeft.x, gridRect.topRight.x, marker.lcp.x),
                        y = lerp(gridRect.bottomLeft.y, gridRect.topRight.y, marker.lcp.y)
                    )
                    val rcp = Offset(
                        x = lerp(gridRect.bottomLeft.x, gridRect.topRight.x, marker.rcp.x),
                        y = lerp(gridRect.bottomLeft.y, gridRect.topRight.y, marker.rcp.y)
                    )

                    val maxExtendedDistance = (Graph.ControlPoint.HandleSize * 1.2f).toPx()
                    val lcpColor = controlPointColor.copy(alpha = min((lcp - target).getDistance() / maxExtendedDistance, 1f))
                    val rcpColor = controlPointColor.copy(alpha = min((rcp - target).getDistance() / maxExtendedDistance, 1f))

                    if (lcpColor.alpha > 0f) {
                        drawLine(color = lcpColor, start = lcp, end = target, strokeWidth = 4.dp.toPx(), pathEffect = pathEffect)
                        drawCircle(color = lcpColor, center = lcp, radius = Graph.ControlPoint.HandleSize.toPx() / 2)
                    }

                    if (lcpColor.alpha < 1f) {
                        val extendedLcp = with(marker.lcp) { toExtendedCenterInRect(gridRect, marker.target, -1f) }
                        drawLine(color = lcpColor.copy(alpha = 1f - lcpColor.alpha), start = extendedLcp, end = target, strokeWidth = 4.dp.toPx(), pathEffect = pathEffect)
                        drawCircle(color = lcpColor.copy(alpha = 1f - lcpColor.alpha), center = extendedLcp, radius = (Graph.ControlPoint.HandleSize - 4.dp).toPx() / 2, style = Stroke(width = 8.dp.toPx()))
                    }

                    if (rcpColor.alpha > 0f) {
                        drawLine(color = rcpColor, start = rcp, end = target, strokeWidth = 4.dp.toPx(), pathEffect = pathEffect)
                        drawCircle(color = rcpColor, center = rcp, radius = Graph.ControlPoint.HandleSize.toPx() / 2)
                    }

                    if (rcpColor.alpha < 1f) {
                        val extendedRcp = with(marker.rcp) { toExtendedCenterInRect(gridRect, marker.target, +1f) }
                        drawLine(color = rcpColor.copy(alpha = 1f - rcpColor.alpha), start = extendedRcp, end = target, strokeWidth = 4.dp.toPx(), pathEffect = pathEffect)
                        drawCircle(color = rcpColor.copy(alpha = 1f - rcpColor.alpha), center = extendedRcp, radius = (Graph.ControlPoint.HandleSize - 4.dp).toPx() / 2, style = Stroke(width = 8.dp.toPx()))
                    }
                }

                drawCircle(
                    color = if (deleting) deleteColor else targetHandleColor,
                    center = target,
                    radius = (Graph.ControlPoint.HandleSize / 2 - (if (selected) 0.dp else 4.dp)).toPx(),
                    style = if (selected) Fill else Stroke(width = 8.dp.toPx())
                )
            }
        }
    }
}