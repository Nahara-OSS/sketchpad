package io.github.naharaoss.skpd.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import kotlin.math.max
import kotlin.math.min

val DpRect.origin: DpOffset get() = DpOffset(left, top)
fun DpRect.moveBy(offset: DpOffset): DpRect = DpRect(origin = origin + offset, size = size)
fun DpRect.moveBy(x: Dp = 0.dp, y: Dp = 0.dp): DpRect = moveBy(DpOffset(x, y))

val Offset.normalized: Offset get() {
    val d = getDistance()
    return Offset(x = x / d, y = y / d)
}

fun <T> List<T>.dropAt(index: Int): List<T> {
    val a = ArrayList<T>(size - 1)

    for (i in 0..lastIndex) {
        if (i == index) continue
        a.add(this[i])
    }

    return a
}

fun <T> List<T>.replaceAt(index: Int, e: T): List<T> = subList(0, index) + e + subList(index + 1, size)
fun <T> Set<T>.toggle(e: T): Set<T> = if (contains(e)) filter { it != e }.toSet() else this + e

fun Rect.union(rect: Rect) = Rect(
    topLeft = Offset(
        x = min(topLeft.x, rect.topLeft.x),
        y = min(topLeft.y, rect.topLeft.y)
    ),
    bottomRight = Offset(
        x = max(bottomRight.x, rect.bottomRight.x),
        y = max(bottomRight.y, rect.bottomRight.y)
    )
)