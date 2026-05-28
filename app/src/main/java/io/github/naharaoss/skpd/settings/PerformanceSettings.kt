package io.github.naharaoss.skpd.settings

import kotlinx.serialization.Serializable

/**
 * Performance settings.
 *
 * These settings allow user to fine-tune the app for the best experience. Some device receives its
 * own different defaults.
 */
@Serializable
data class PerformanceSettings(
    /**
     * Default log2 of tile size (from 6 to 12).
     *
     * The size of the tile controls how much memory to use, as well as how often the app need to
     * perform I/O operations to load the tiles. Smaller tile will require more tile loading and
     * draw calls, while bigger tile may use more GPU memory.
     *
     * The valid tile sizes are: 64, 128, 256, 512, 1024, 2048 and 4096, which corresponding to log2
     * value 6, 7, 8, 9, 10, 11 and 12, respectively.
     */
    val defaultTileSizeLog: Int = 7,

    /**
     * Tile preloading ratio.
     *
     * This expands the bounds of the screen to covers the tiles that are outside the screen and
     * preload them. A high preload ratio will use more GPU memory, while a low preload ratio will
     * perform more I/O operations. A balanced ratio makes canvas smooth to pan and rotate around.
     * A good ratio depends on how fast user is panning around, how big the display is and how fast
     * the system can perform I/O operations.
     */
    val preloadRatio: Float = 0.20f,

    /**
     * Minimum zoom level for infinite canvas.
     *
     * The minimum zoom level that user can zoom out when drawing in infinite canvas. Smaller value
     * allows zooming out more, but it also needs to load even more tiles, and it also scales with
     * preload ratio, which leads to more GPU memory usage and draw calls.
     */
    val infiniteCanvasMinZoom: Float = 0.5f,

    /**
     * Minimum zoom level for sized canvas.
     *
     * Unlike infinite canvas, sized canvas can simply ignore any tiles that are outside the bounds
     * of the canvas.
     */
    val sizedCanvasMinZoom: Float = 0.01f,

    /**
     * Maximum number of undo (from 0 to 200).
     *
     * The edit history is stored directly into the document, and if there are too many edits, the
     * file size will get really huge.
     */
    val maxUndoCount: Int = 50,
)
