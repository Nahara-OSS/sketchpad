package io.github.naharaoss.skpd.document.graphics

import androidx.annotation.WorkerThread
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.document.DocumentAccess
import io.github.naharaoss.skpd.utils.GLFramebuffer
import io.github.naharaoss.skpd.utils.TileAddress
import io.github.naharaoss.skpd.utils.calculateVisibleTiles

@WorkerThread
class DocumentRenderer(
    val document: DocumentAccess
) : AutoCloseable {
    private var loadedTiles = emptySet<TileAddress>()
    private val layerRenderers = mutableMapOf<DocumentAccess.Layer, LayerRenderer>()
    private val tileAllocator = TileTexture.createAllocator(document.tileSize)
    private val tileProgram = TileProgram()

    val layers get() = layerRenderers.toMap()

    fun update(viewport: Rect, canvasTransform: Matrix, z: Int = 0) {
        val visibleTiles = calculateVisibleTiles(
            viewport = viewport.deflate(200f),
            canvasSize = document.size,
            canvasTransform = canvasTransform,
            tileSize = document.tileSize,
            z = z
        )

        val loadTiles = visibleTiles.subtract(loadedTiles)
        val unloadTiles = loadedTiles.subtract(visibleTiles)
        val unloadLayers = document.layers.toMutableSet()
        loadedTiles = visibleTiles

        for (layer in document.layers) {
            val renderer = layerRenderers[layer] ?: LayerRenderer(document.tileSize, tileAllocator, layer).also { layerRenderers[layer] = it }
            loadTiles.forEach { tile -> layer.preloadTile(tile) }
            unloadTiles.forEach { tile -> layer.unloadTile(tile) }
            renderer.update(loadTiles, unloadTiles)
            unloadLayers.remove(layer)
        }

        for (layer in unloadLayers) {
            layerRenderers[layer]?.close()
            layerRenderers.remove(layer)
        }

        tileAllocator.cleanUp(false)
    }

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
        loadedTiles = emptySet()
        layerRenderers.forEach { (_, renderer) -> renderer.close() }
        layerRenderers.clear()
        tileAllocator.cleanUp(true)
        tileProgram.close()
    }
}