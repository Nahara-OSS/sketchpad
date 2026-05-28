package io.github.naharaoss.skpd.utils

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * The address of a tile.
 */
@Serializable
data class TileAddress(
    /**
     * The X position of the tile.
     *
     * The X position of the tile in tile coordinates space. Convert from tile coordinates to pixel
     * coordinates by multiplying the tile X position by the size of the tile.
     */
    val x: Int,

    /**
     * The Y position of the tile.
     *
     * The Y position of the tile in tile coordinates space. Convert from tile coordinates to pixel
     * coordinates by multiplying the tile Y position by the size of the tile.
     */
    val y: Int,

    /**
     * The frame number of the tile.
     *
     * Called as "Z", but this is for the frame number of the tile.
     */
    val z: Int
)

fun ByteBuffer.putTileAddress(address: TileAddress): ByteBuffer = this
    .putInt(address.x)
    .putInt(address.y)
    .putInt(address.z)

fun ByteBuffer.getTileAddress(): TileAddress = TileAddress(
    x = getInt(),
    y = getInt(),
    z = getInt()
)

/**
 * Calculate all visible tiles from given viewport information.
 *
 * Use this to find all tiles that will be visible inside the viewport.
 *
 * @param [viewport] The size of viewport
 * @param [canvasSize] The size of the canvas
 * @param [canvasTransform] The transformation of the canvas
 * @param [tileSize] The size of a tile
 * @param [z] The Z value for all tiles
 */
fun calculateVisibleTiles(
    viewport: Rect,
    canvasSize: Size,
    canvasTransform: Matrix,
    tileSize: Int,
    z: Int = 0
): Set<TileAddress> {
    val viewport = viewport
    val inverse = Matrix(canvasTransform.values.clone()).apply { invert() }
    val canvasBounds = inverse.map(viewport)
    val result = mutableSetOf<TileAddress>()

    for (y in (canvasBounds.top / tileSize).roundToInt() - 1..(canvasBounds.bottom / tileSize).roundToInt()) {
        for (x in (canvasBounds.left / tileSize).roundToInt() - 1..(canvasBounds.right / tileSize).roundToInt()) {
            if (canvasSize is Size.Sized) {
                val xInCanvas = (x * tileSize + tileSize) >= -(canvasSize.width / 2) && x * tileSize <= canvasSize.width / 2
                val yInCanvas = (y * tileSize + tileSize) >= -(canvasSize.height / 2) && y * tileSize <= canvasSize.height / 2
                if (!(xInCanvas && yInCanvas)) continue
            }

            val topLeft = Offset(x = (x * tileSize).toFloat(), y = (y * tileSize).toFloat())
            val bottomRight = topLeft + Offset(x = tileSize.toFloat(), y = tileSize.toFloat())

            if (canvasTransform.map(Rect(topLeft, bottomRight)).overlaps(viewport)) {
                result.add(TileAddress(x = x, y = y, z = z))
            }
        }
    }

    return result
}