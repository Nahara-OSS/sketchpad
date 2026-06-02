package io.github.naharaoss.skpd.document.graphics

import android.opengl.GLES30
import androidx.annotation.WorkerThread
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.document.DocumentAccess
import io.github.naharaoss.skpd.utils.GLFramebuffer
import io.github.naharaoss.skpd.utils.Size
import io.github.naharaoss.skpd.utils.TileAddress
import io.github.naharaoss.skpd.utils.calculateVisibleTiles

@WorkerThread
class DocumentRenderer(val document: DocumentAccess) : AutoCloseable {
    internal var visibleTiles = emptySet<TileAddress>()
    private val layerRenderers = mutableMapOf<DocumentAccess.Layer, LayerRenderer>()
    private val tileProgram = TileProgram()
    private val backgroundProgram = CanvasBackgroundProgram()

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
        val unloadLayers = layerRenderers.keys.toMutableSet()
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
     * @param [background] Background color of drawing board
     * @param [stencil] Whether to use stencil to cut the canvas (usually disabled when exporting)
     */
    fun render(
        viewport: Rect,
        canvasTransform: Matrix,
        framebuffer: GLFramebuffer,
        background: Color = document.background,
        stencil: Boolean = true,
    ) {
        val documentSize = document.size

        framebuffer.bind {
            when (documentSize) {
                is Size.Sized -> {
                    if (stencil) {
                        GLES30.glEnable(GLES30.GL_STENCIL_TEST)
                        setClearStencil(0x00)
                        setClearColor(background)
                        clear(GLFramebuffer.ClearType.Color, GLFramebuffer.ClearType.Stencil)

                        GLES30.glStencilFunc(GLES30.GL_ALWAYS, 1, 0xFF)
                        GLES30.glStencilOp(GLES30.GL_KEEP, GLES30.GL_KEEP, GLES30.GL_REPLACE)
                        GLES30.glStencilMask(0xFF)
                    } else {
                        setClearColor(background)
                        clear(GLFramebuffer.ClearType.Color)
                    }

                    backgroundProgram.draw(
                        viewport = viewport,
                        canvasTransform = canvasTransform,
                        canvasSize = documentSize,
                        color = document.background
                    )

                    if (stencil) {
                        GLES30.glStencilFunc(GLES30.GL_EQUAL, 1, 0xFF)
                        GLES30.glStencilMask(0x00)
                    }
                }

                is Size.Infinite -> {
                    setClearColor(document.background)
                    clear(GLFramebuffer.ClearType.Color)
                }
            }
        }

        for ((layer, layerRenderer) in layerRenderers) {
            if (layer.visible) layerRenderer.render(
                tileProgram = tileProgram,
                viewport = viewport,
                canvasTransform = canvasTransform,
                framebuffer = framebuffer
            )
        }

        if (documentSize is Size.Sized && stencil) {
            GLES30.glDisable(GLES30.GL_STENCIL_TEST)
        }
    }

    override fun close() {
        visibleTiles = emptySet()
        layerRenderers.forEach { (_, renderer) -> renderer.close() }
        layerRenderers.clear()
        tileProgram.close()
        backgroundProgram.close()
    }
}