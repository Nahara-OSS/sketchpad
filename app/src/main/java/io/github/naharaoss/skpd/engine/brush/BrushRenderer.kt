package io.github.naharaoss.skpd.engine.brush

import androidx.annotation.WorkerThread
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix

/**
 * **All methods must be called inside thread with OpenGL context!** Always assume all methods will
 * interact with OpenGL upon calling.
 */
@WorkerThread
interface BrushRenderer<T : Brush> : AutoCloseable {
    /**
     * Signal the renderer to begin a stroke. This will reset the internal state of brush renderer.
     *
     * @param brush The brush preset that will be used to create a stroke. Update the state of
     * renderer according to the brush preset.
     */
    fun begin(brush: T)

    /**
     * Unchecked version of [begin].
     */
    @Suppress("UNCHECKED_CAST")
    fun uncheckedBegin(brush: Brush) = begin(brush as T)

    fun consume(inputs: List<StylusInput>): Rect?

    /**
     * Called on every tiles whose hitbox intersects with rectangle returned from [consume]. Unlike
     * [renderToCanvas], the implementation may override this to draw to stroke layer first before
     * compositing to canvas in [renderToCanvas].
     */
    fun processTile(
        worldToView: Matrix,
        key: Any,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    )

    /**
     * Called on every tiles to render the stroke to canvas's framebuffer.
     */
    fun renderToCanvas(
        canvasFb: Int,
        worldToView: Matrix,
        key: Any,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    )
}