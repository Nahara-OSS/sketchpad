package io.github.naharaoss.skpd.document

import androidx.compose.ui.graphics.Color
import io.github.naharaoss.skpd.utils.BlendMode
import io.github.naharaoss.skpd.utils.Size
import io.github.naharaoss.skpd.utils.TileAddress
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MemoryDocument(
    override val tileSizeLog: Int,
    override var size: Size,
    override var background: Color
) : DocumentAccess {
    private val lock = ReentrantLock()
    private val _layers = mutableListOf<Layer>()
    override val layers: List<Layer> get() = _layers.toList()

    override fun openWriter(): DocumentAccess.Writer = Writer(this)

    fun addLayer(name: String = "Layer ${_layers.size + 1}"): Layer {
        val layer = Layer(
            document = this,
            name = name,
            visible = true,
            blend = BlendMode.SourceOver,
            opacity = 1f
        )
        lock.withLock { _layers.add(layer) }
        return layer
    }

    class Layer(
        private val document: MemoryDocument,
        var name: String,
        override var visible: Boolean,
        override var blend: BlendMode,
        override var opacity: Float
    ) : DocumentAccess.Layer {
        val lock = ReentrantLock()
        val tiles = mutableMapOf<TileAddress, ByteArray>()

        override fun preloadTile(address: TileAddress) {}
        override fun unloadTile(address: TileAddress) {}
        override fun isTileExists(address: TileAddress): Boolean = tiles.contains(address)

        override fun loadTile(address: TileAddress, dst: ByteBuffer) {
            if (dst.remaining() < document.bytesPerTile) throw Exception("Buffer does not have enough space to store data")
            val tile = tiles[address] ?: throw Exception("No such tile at $address")
            lock.withLock { dst.put(tile) }
        }
    }

    private class Writer(
        private val document: MemoryDocument
    ) : DocumentAccess.Writer {
        override fun DocumentAccess.Layer.storeTile(address: TileAddress, src: ByteBuffer) {
            if (this !is Layer) throw Exception("Called on incorrect layer implementation")
            if (src.remaining() < document.bytesPerTile) throw Exception("Buffer does not have enough remaining bytes to read data")
            val array = ByteArray(document.bytesPerTile)
            src.get(array)
            lock.withLock { tiles[address] = array }
        }

        override fun close() {}
    }
}