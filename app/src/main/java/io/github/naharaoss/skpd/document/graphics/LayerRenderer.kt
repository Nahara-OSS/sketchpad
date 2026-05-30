package io.github.naharaoss.skpd.document.graphics

import android.opengl.GLES30
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.document.DocumentAccess
import io.github.naharaoss.skpd.utils.BlendMode
import io.github.naharaoss.skpd.utils.GLFramebuffer
import io.github.naharaoss.skpd.utils.TileAddress
import io.github.naharaoss.skpd.utils.toBlendState
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Renderer for layer in document.
 *
 * Layers are created and destroyed by [DocumentRenderer] when [DocumentRenderer.update] is called.
 *
 * - To use brush (drawing) on layer, use [useBrush] with a loaded brush renderer.
 * - To commit the brush stroke to document's layer, call [commitBrush] on [DocumentAccess.Writer].
 * - To cancel the brush stroke (eg: user activated gesture instead of drawing), call [cancelBrush].
 */
class LayerRenderer internal constructor(val parent: DocumentRenderer, val layer: DocumentAccess.Layer) {
    private val tileSize = parent.document.tileSize
    private val tiles = mutableMapOf<TileAddress, TileTexture>()
    private val pendingTiles = mutableMapOf<TileAddress, TileTexture>()

    internal fun update(loadTiles: Set<TileAddress>, unloadTiles: Set<TileAddress>) {
        val buffer = ByteBuffer.allocateDirect(tileSize * tileSize * 4).order(ByteOrder.nativeOrder())

        loadTiles.forEach { address ->
            if (!layer.isTileExists(address)) return@forEach
            buffer.clear()
            layer.loadTile(address, buffer)
            buffer.flip()
            tiles[address] = tiles[address] ?: TileTexture(tileSize, buffer)
        }

        unloadTiles.forEach { address ->
            tiles.remove(address)?.close()
            pendingTiles.remove(address)?.close()
        }
    }

    /**
     * Use brush on this layer.
     *
     * [BrushType.Renderer.consumeInput] must be called before using the brush with this layer,
     * while [BrushType.Renderer.consumeTile] must not be called as it will be called by this method
     * instead.
     *
     * To write the brush stroke to document, use [commitBrush].
     *
     * @param [address] Address of the tile being touched by brush
     * @param [brushRenderer] The brush renderer
     */
    fun useBrush(address: TileAddress, brushRenderer: BrushType.Renderer<*>) {
        val tile = tiles[address]
        val pendingTile = pendingTiles[address] ?: TileTexture(tileSize, null)
        pendingTiles[address] = pendingTile
        brushRenderer.consumeTile(address, address.calculateTileRect(tileSize))

        if (tile != null) {
            tile.framebuffer.bind {
                pendingTile.texture.bind {
                    GLES30.glCopyTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, 0, 0, tileSize, tileSize)
                }
            }
        } else {
            pendingTile.framebuffer.bind {
                setViewport(0, 0, tileSize, tileSize)
                setClearColor(Color.Transparent)
                clear(GLFramebuffer.ClearType.Color)
            }
        }

        pendingTile.framebuffer.bind {
            setViewport(0, 0, tileSize, tileSize)
        }

        GLES30.glEnable(GLES30.GL_BLEND)
        BlendMode.SourceOver.toBlendState().use()

        brushRenderer.renderTile(
            tileKey = address,
            tileRect = address.calculateTileRect(tileSize),
            framebuffer = pendingTile.framebuffer,
            transform = Matrix().apply { scale(y = -1f) }
        )

        GLES30.glDisable(GLES30.GL_BLEND)
    }

    /**
     * Commit the brush stroke on this layer to document.
     */
    fun DocumentAccess.Writer.commitBrush() {
        val buffer = ByteBuffer.allocateDirect(tileSize * tileSize * 4)

        for ((address, tile) in pendingTiles) {
            tiles[address]?.close()
            tiles[address] = tile
            tile.framebuffer.bind {
                GLES30.glReadPixels(
                    0, 0,
                    tileSize, tileSize,
                    GLES30.GL_RGBA,
                    GLES30.GL_UNSIGNED_BYTE,
                    buffer
                )
            }
            layer.storeTile(address, buffer)
            buffer.clear()
        }

        pendingTiles.clear()
    }

    /**
     * Cancel pending brush stroke on this layer.
     */
    fun cancelBrush() {
        pendingTiles.forEach { (_, tile) -> tile.close() }
        pendingTiles.clear()
    }

    internal fun render(
        tileProgram: TileProgram,
        viewport: Rect,
        canvasTransform: Matrix,
        framebuffer: GLFramebuffer
    ) {
        if (layer.opacity == 0f) return

        for (address in tiles.keys + pendingTiles.keys) {
            val tile = tiles[address]
            val pendingTile = pendingTiles[address]
            val displayTile = pendingTile ?: tile

            if (displayTile != null) {
                framebuffer.bind {
                    GLES30.glEnable(GLES30.GL_BLEND)
                    layer.blend.toBlendState().use()

                    tileProgram.draw(
                        source = displayTile.texture,
                        tileSize = tileSize,
                        viewport = viewport,
                        canvasTransform = canvasTransform,
                        tileAddress = address
                    )

                    GLES30.glDisable(GLES30.GL_BLEND)
                }
            }
        }
    }

    internal fun close() {
        tiles.forEach { (_, tile) -> tile.close() }
        tiles.clear()
        pendingTiles.forEach { (_, tile) -> tile.close() }
        pendingTiles.clear()
    }
}