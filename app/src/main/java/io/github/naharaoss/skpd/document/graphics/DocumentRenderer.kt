package io.github.naharaoss.skpd.document.graphics

import androidx.annotation.WorkerThread
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.document.DocumentAccess
import io.github.naharaoss.skpd.utils.GLFramebuffer
import io.github.naharaoss.skpd.utils.TileAddress
import io.github.naharaoss.skpd.utils.calculateVisibleTiles

@WorkerThread
class DocumentRenderer(val document: DocumentAccess) : AutoCloseable {
    internal var visibleTiles = emptySet<TileAddress>()
    private val layerRenderers = mutableMapOf<DocumentAccess.Layer, LayerRenderer>()
    private val tileProgram = TileProgram()

    /**
     * A map of loaded layers.
     */
    val layers get() = layerRenderers.toMap()

    /**
     * Update the renderer.
     *
     * This method will collect the address of tiles that are visible in the viewport and load or
     * unload resources based on the changes of tile's visibility.
     *
     * @param [viewport] The viewport rectangle
     * @param [canvasTransform] The transformation of canvas
     * @param [z] Pass `0` to parameter
     */
    fun update(viewport: Rect, canvasTransform: Matrix, z: Int = 0) {
        val newVisibleTiles = calculateVisibleTiles(
            viewport = viewport,
            canvasSize = document.size,
            canvasTransform = canvasTransform,
            tileSize = document.tileSize,
            z = z
        )

        val loadTiles = newVisibleTiles.subtract(visibleTiles)
        val unloadTiles = visibleTiles.subtract(newVisibleTiles)
        val unloadLayers = document.layers.toMutableSet()
        visibleTiles = newVisibleTiles

        for (layer in document.layers) {
            val renderer = layerRenderers.getOrPut(layer, { LayerRenderer(this, layer) })
            renderer.update(loadTiles, unloadTiles)
            unloadLayers.remove(layer)
        }

        for (layer in unloadLayers) {
            layerRenderers[layer]?.close()
            layerRenderers.remove(layer)
        }
    }

    /**
     * Render to framebuffer.
     *
     * Make sure to call [update] when the viewport is changed before rendering.
     *
     * @param [viewport] The viewport rectangle
     * @param [canvasTransform] The transformation of canvas
     * @param [framebuffer] The target framebuffer to render into
     */
    fun render(
        viewport: Rect,
        canvasTransform: Matrix,
        framebuffer: GLFramebuffer
    ) {
        for ((_, layer) in layerRenderers) {
            layer.render(
                tileProgram = tileProgram,
                viewport = viewport,
                canvasTransform = canvasTransform,
                framebuffer = framebuffer
            )
        }
    }

    override fun close() {
        visibleTiles = emptySet()
        layerRenderers.forEach { (_, renderer) -> renderer.close() }
        layerRenderers.clear()
        tileProgram.close()
    }
}