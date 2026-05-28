package io.github.naharaoss.skpd.document

import io.github.naharaoss.skpd.utils.BlendMode
import io.github.naharaoss.skpd.utils.ColorSpace
import io.github.naharaoss.skpd.utils.Size
import kotlinx.serialization.Serializable

/**
 * Interface for all implementations of Sketchpad document.
 *
 * **Tile-based architecture**: Nahara's Sketchpad uses tiles system in order to support infinite
 * canvas. Untouched tiles (tiles that aren't being drawn on by user) will be treated as fully
 * transparent tile (which is ignored by renderer). When loading tiles, the tiles inside visible
 * area must be loaded (spinlock will be used to ensure they are fully loaded), while tiles outside
 * the visible area but inside the preload area will be loaded in background. Tiles that are outside
 * the preload area will be stored to disk and then unloaded.
 *
 * **Tile data format**: The buffer size of the tile can be calculated by multiplying square of
 * [tileSize] by 4, where 4 is for each pixel on the tile. The pixel format is RGBA.
 *
 * **I/O operations**: All methods and setters in here are blocking and must be performed in I/O
 * thread.
 */
interface SketchpadDocument : AutoCloseable {
    val tileSizeLog: Int
    val tileSize get() = 1 shl tileSizeLog
    val tileByteCount get() = tileSize * tileSize * 4
    var properties: DocumentProperties
    val history: HistoryInfo

    fun undo()
    fun redo()

    data class HistoryInfo(
        val undo: Int,
        val redo: Int
    )

    @Serializable
    data class DocumentProperties(
        val size: Size,
        val colorSpace: ColorSpace,
        val frameRate: Int
    )

    @Serializable
    data class LayerProperties(
        val name: String,
        val blending: BlendMode,
        val visible: Boolean,
        val opacity: Float
    )
}