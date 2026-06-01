package io.github.naharaoss.skpd.document

import androidx.compose.ui.graphics.Color
import io.github.naharaoss.skpd.utils.BlendMode
import io.github.naharaoss.skpd.utils.Size
import io.github.naharaoss.skpd.utils.TileAddress
import java.nio.ByteBuffer

/**
 * Interface for accessing the document content.
 *
 * This interface is meant to be used in render thread, where the content need to be pulled from the
 * document to renderer's internal state.
 */
interface DocumentAccess {
    val tileSizeLog: Int
    val tileSize get() = 1 shl tileSizeLog
    val bytesPerTile get() = tileSize * tileSize * 4
    val size: Size
    val layers: List<Layer>
    val background: Color

    /**
     * Open document writer.
     *
     * The writer collect all tile store commands and combine them into a single history item in the
     * history stack. To commit to history stack, the writer must be closed. The order of history
     * item depends on close order of writer.
     */
    fun openWriter(): Writer

    interface Layer {
        val visible: Boolean
        val opacity: Float
        val blend: BlendMode

        /**
         * Check whether there is a tile at address.
         */
        fun isTileExists(address: TileAddress): Boolean

        /**
         * Mark the tile for preloading.
         *
         * This is a hint function to tell the implementation to prepare the tile. The renderer may
         * mark some tiles outside the viewport for loading ahead of time.
         */
        fun preloadTile(address: TileAddress)

        /**
         * Mark the tile for unloading.
         *
         * This is a hint function to tell the implementation to unload the preloaded tile.
         */
        fun unloadTile(address: TileAddress)

        /**
         * Load the content of the tile.
         *
         * This function may block the execution to fully load the content of the tile. Once the
         * tile is loaded, if the tile is not marked for preloading, it should be discarded
         * immediately.
         */
        fun loadTile(address: TileAddress, dst: ByteBuffer)
    }

    interface Writer : AutoCloseable {
        fun Layer.storeTile(address: TileAddress, src: ByteBuffer)
    }
}