package io.github.naharaoss.skpd.document

import androidx.compose.ui.graphics.Color
import io.github.naharaoss.container.ByteChannelUtils
import io.github.naharaoss.container.ContainerDocument
import io.github.naharaoss.skpd.document.SketchpadDocumentV1.Companion.CHUNK_TYPE_LAYER_STACK
import io.github.naharaoss.skpd.document.SketchpadDocumentV1.Companion.CHUNK_TYPE_METADATA
import io.github.naharaoss.skpd.utils.BlendMode
import io.github.naharaoss.skpd.utils.ColorSerializer
import io.github.naharaoss.skpd.utils.Size
import io.github.naharaoss.skpd.utils.TileAddress
import io.github.naharaoss.skpd.utils.UUIDSerializer
import io.github.naharaoss.skpd.utils.alignTo
import io.github.naharaoss.skpd.utils.allocateWithJson
import io.github.naharaoss.skpd.utils.decodeJson
import io.github.naharaoss.skpd.utils.getInt24
import io.github.naharaoss.skpd.utils.getUUID
import io.github.naharaoss.skpd.utils.putInt24
import io.github.naharaoss.skpd.utils.putUUID
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Version 1 of Nahara's Sketchpad Document.
 */
class SketchpadDocumentV1 private constructor(private val container: ContainerDocument) : DocumentAccess, AutoCloseable {
    private val ioLock = ReentrantLock()
    private var _metadata: Metadata
    private var _layerStack: LayerStack
    private val _layers = mutableMapOf<UUID, Layer>()

    private var metadata
        get() = _metadata
        set(value) {
            _metadata = value
            ioLock.withLock {
                val metadataChunk = container.data.find { it.type == CHUNK_TYPE_METADATA } ?: throw Exception("Missing metadata chunk")
                container.delete(metadataChunk)
                container.allocateWithJson(CHUNK_TYPE_METADATA, value)
            }
        }

    private var layerStack
        get() = _layerStack
        set(value) {
            _layerStack = value
            ioLock.withLock {
                val layerStackChunk = container.data.find { it.type == CHUNK_TYPE_LAYER_STACK } ?: throw Exception("Missing layer stack chunk")
                container.delete(layerStackChunk)
                container.allocateWithJson(CHUNK_TYPE_LAYER_STACK, value)
            }
        }

    override val size: Size get() = metadata.size
    override val tileSizeLog: Int get() = metadata.tileSizeLog
    override val layers: List<Layer> get() = _layerStack.layers.map { _layers[it.id]!! }

    override var background: Color
        get() = metadata.background
        set(value) { metadata = metadata.copy(background = value) }

    var activeLayer: Layer?
        get() = layerStack.active?.let { _layers[it] }
        set(value) { layerStack = layerStack.copy(active = value?.id) }

    init {
        val namespace = container.namespace.toString(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        if (namespace != NAMESPACE) throw Exception("Namespace is unexpected: $namespace != $NAMESPACE")
        _metadata = container.decodeJson(container.data.find { it.type == CHUNK_TYPE_METADATA } ?: throw Exception("Missing metadata chunk"))
        _layerStack = container.decodeJson(container.data.find { it.type == CHUNK_TYPE_LAYER_STACK } ?: throw Exception("Missing layer stack chunk"))

        val layerChunks = container.data.filter { it.type == CHUNK_TYPE_LAYER }.associateBy {
            buffer.clear().limit(16)
            container.channel.position(it.offset)
            ByteChannelUtils.readFully(container.channel, buffer)
            buffer.flip()
            buffer.getUUID()
        }

        for (layer in _layerStack.layers) {
            _layers[layer.id] = Layer(this, layer.id, layerChunks[layer.id]!!)
        }
    }

    fun addLayer(
        name: String = "Layer ${_layerStack.layers.size + 1}",
        visible: Boolean = true,
        opacity: Float = 1f,
        blend: BlendMode = BlendMode.SourceOver
    ): Layer {
        val id = UUID.randomUUID()
        val info = LayerStack.LayerInfo(id, name, visible, blend, opacity)
        val buffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putUUID(id) // Layer ID
        buffer.putInt(0) // Tile count
        buffer.flip()

        val layerChunk = ioLock.withLock {
            val layerChunk = container.allocate(CHUNK_TYPE_LAYER, 1024)
            container.channel.position(layerChunk.offset)
            container.channel.write(buffer)
            layerChunk
        }

        val layer = Layer(this, id, layerChunk)
        _layers[id] = layer
        layerStack = layerStack.copy(layers = layerStack.layers + info)
        return layer
    }

    override fun openWriter(): DocumentAccess.Writer {
        return Writer(this)
    }

    override fun close() {
        // TODO: Wait for queue to finish
        container.close()
    }

    /**
     * Chunk type is [CHUNK_TYPE_METADATA]
     */
    @Serializable
    private data class Metadata(
        val tileSizeLog: Int,
        val size: Size,
        @Serializable(with = ColorSerializer::class) val background: Color
    )

    /**
     * Chunk type is [CHUNK_TYPE_LAYER_STACK]
     */
    @Serializable
    private data class LayerStack(
        val layers: List<LayerInfo>,
        @Serializable(with = UUIDSerializer::class) val active: UUID?
    ) {
        @Serializable
        data class LayerInfo(
            @Serializable(with = UUIDSerializer::class) val id: UUID,
            val name: String,
            val visible: Boolean,
            val blend: BlendMode,
            val opacity: Float
        )
    }

    class Layer internal constructor(
        private val document: SketchpadDocumentV1,
        val id: UUID,
        internal var chunk: ContainerDocument.Data
    ) : DocumentAccess.Layer {
        private var layerInfo
            get() = document.layerStack.layers.first { it.id == id }
            set(value) {
                val layerStack = document.layerStack
                val layers = layerStack.layers.map { if (it.id == id) value else it }
                document.layerStack = layerStack.copy(layers = layers)
            }

        internal val tiles = mutableMapOf<TileAddress, SavedTile>()

        var name: String
            get() = layerInfo.name
            set(value) { layerInfo = layerInfo.copy(name = value) }

        override var visible: Boolean
            get() = layerInfo.visible
            set(value) { layerInfo = layerInfo.copy(visible = value) }

        override var blend: BlendMode
            get() = layerInfo.blend
            set(value) { layerInfo = layerInfo.copy(blend = value) }

        override var opacity: Float
            get() = layerInfo.opacity
            set(value) { layerInfo = layerInfo.copy(opacity = value) }

        init {
            val buffer = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN)

            val addressToTileId = document.ioLock.withLock {
                buffer.clear().limit(4)
                document.container.channel.position(chunk.offset + 16)
                ByteChannelUtils.readFully(document.container.channel, buffer)
                buffer.flip()
                val tiles = buffer.getInt()

                (0..<tiles).associate {
                    buffer.clear().limit(28)
                    ByteChannelUtils.readFully(document.container.channel, buffer)
                    buffer.flip()
                    val x = buffer.getInt()
                    val y = buffer.getInt()
                    val z = buffer.getInt()
                    val tileId = buffer.getUUID()
                    val address = TileAddress(x, y, z)
                    address to tileId
                }
            }

            for ((address, id) in addressToTileId) {
                val chunk = document.container.data.first {
                    if (it.type != CHUNK_TYPE_TILE) return@first false
                    buffer.clear().limit(16)
                    document.container.channel.position(it.offset)
                    ByteChannelUtils.readFully(document.container.channel, buffer)
                    buffer.flip()
                    val tileId = buffer.getUUID()
                    tileId == id
                }

                tiles[address] = SavedTile(chunk, id)
            }
        }

        override fun preloadTile(address: TileAddress) {
            // TODO
        }

        override fun unloadTile(address: TileAddress) {
            // TODO
        }

        override fun isTileExists(address: TileAddress): Boolean = tiles.contains(address)

        override fun loadTile(address: TileAddress, dst: ByteBuffer) {
            if (dst.remaining() < document.bytesPerTile) throw Exception("Destination buffer must have at least ${document.bytesPerTile} bytes remaining (currently ${dst.remaining()})")
            val tile = tiles[address] ?: throw Exception("No tile at address $address")
            val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)

            document.ioLock.withLock {
                document.container.channel.position(tile.chunk.offset + 16)
                ByteChannelUtils.readFully(document.container.channel, buffer)
                buffer.flip()
                val compressionType = CompressionType.entries[buffer.get().toInt() and 0xFF]
                val size = buffer.getInt24()

                if (compressionType == CompressionType.Uncompressed) {
                    if (size != document.bytesPerTile) throw Exception("Tile at $address have uncompressed size of $size (bytes per uncompressed tile must be ${document.bytesPerTile})")
                    ByteChannelUtils.readFully(document.container.channel, dst)
                } else {
                    TODO("Implement tile compression")
                }
            }
        }

        /**
         * Delete layer from the document.
         */
        fun delete() {
            document.layerStack = document.layerStack.copy(layers = document.layerStack.layers.dropWhile { it.id == id })

            document.ioLock.withLock {
                document.container.delete(chunk)
                for ((address, tile) in tiles) document.container.delete(tile.chunk)
            }
        }
    }

    private class Writer(
        private val document: SketchpadDocumentV1
    ) : DocumentAccess.Writer {
        private val pendingLayers = mutableMapOf<Layer, MutableMap<TileAddress, SavedTile>>()

        override fun DocumentAccess.Layer.storeTile(address: TileAddress, src: ByteBuffer) {
            if (this !is Layer) throw Exception("Calling on wrong Layer implementation")
            if (src.remaining() < document.bytesPerTile) throw Exception("Source buffer must have at least ${document.bytesPerTile} bytes remaining (currently ${src.remaining()})")
            val pendingTiles = pendingLayers.getOrPut(this, { mutableMapOf() })
            val tile = pendingTiles[address]
            val tileId = tile?.id ?: UUID.randomUUID()

            val buffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
            buffer.clear().limit(20)
            buffer.putUUID(tileId)
            buffer.put(CompressionType.Uncompressed.ordinal.toByte())
            buffer.putInt24(document.bytesPerTile)
            buffer.flip()

            document.ioLock.withLock {
                val chunk = if (tile != null) {
                    val chunk = document.container.resize(tile.chunk, 20 + document.bytesPerTile)
                    pendingTiles[address] = tile.copy(chunk = chunk)
                    chunk
                } else {
                    val chunk = document.container.allocate(CHUNK_TYPE_TILE, 20 + document.bytesPerTile)
                    pendingTiles[address] = SavedTile(chunk, tileId)
                    chunk
                }

                document.container.channel.position(chunk.offset)
                ByteChannelUtils.writeFully(document.container.channel, buffer)
                ByteChannelUtils.writeFully(document.container.channel, src)
            }
        }

        override fun close() {
            if (pendingLayers.isEmpty() || pendingLayers.values.all { it.isEmpty() }) return
            val buffer = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN)

            for ((layer, pendingTiles) in pendingLayers) {
                val discarding = mutableSetOf<SavedTile>()

                for ((address, tile) in pendingTiles) {
                    layer.tiles[address]?.let { discarding.add(it) }
                    layer.tiles[address] = tile
                }

                document.ioLock.withLock {
                    val layerChunk = document.container.resize(layer.chunk, (20 + 28 * layer.tiles.size).alignTo(1024))
                    layer.chunk = layerChunk

                    buffer.clear().limit(20)
                    buffer.putUUID(layer.id)
                    buffer.putInt(layer.tiles.size)
                    buffer.flip()
                    document.container.channel.position(layerChunk.offset)
                    ByteChannelUtils.writeFully(document.container.channel, buffer)

                    for ((address, tile) in layer.tiles) {
                        buffer.clear().limit()
                        buffer.putInt(address.x)
                        buffer.putInt(address.y)
                        buffer.putInt(address.z)
                        buffer.putUUID(tile.id)
                        buffer.flip()
                        ByteChannelUtils.writeFully(document.container.channel, buffer)
                    }

                    for (tile in discarding) {
                        document.container.delete(tile.chunk)
                    }
                }
            }
        }
    }

    internal data class SavedTile(
        val chunk: ContainerDocument.Data,
        val id: UUID
    )

    enum class CompressionType {
        Uncompressed {
            override fun compress(raw: ByteArray): ByteArray = throw Exception("Please handle uncompressed type manually")
            override fun decompress(compressed: ByteArray, expectedSize: Int) = throw Exception("Please handle uncompressed type manually")
        };

        abstract fun compress(raw: ByteArray): ByteArray

        abstract fun decompress(compressed: ByteArray, expectedSize: Int): ByteArray
    }

    companion object {
        const val NAMESPACE = "nahara:sketchpad/v1"

        /**
         * ID for metadata chunk.
         *
         * This chunk is JSON-encoded, containing the general information about the document. Some
         * fields must not be modified (such as tile size), while some can be altered (like canvas
         * size or background for example).
         */
        const val CHUNK_TYPE_METADATA = 0x0001

        /**
         * ID for layer stack chunk.
         *
         * This chunk is JSON-encoded, containing the information about layer stack. Version 1 of
         * Nahara's Sketchpad Document only specify a basic layer stack, which is an array of layers
         * ordered by its stacking order.
         */
        const val CHUNK_TYPE_LAYER_STACK = 0x0002

        /**
         * ID for layer chunk.
         *
         * Each layer is associated with a chunk, and the chunk is encoded as follows:
         *
         * | Offset        | Data type | Name                    |
         * | ------------- | --------- | ----------------------- |
         * | 0             | u64       | LSB of layer's UUID     |
         * | 8             | u64       | MSB of layer's UUID     |
         * | 16            | u32       | Number of tiles         |
         * | 20 + 28n + 0  | u32       | nth tile's X coordinate |
         * | 20 + 28n + 4  | u32       | nth tile's Y coordinate |
         * | 20 + 28n + 8  | u32       | nth tile's Z coordinate |
         * | 20 + 28n + 12 | u64       | LSB of tile's UUID      |
         * | 20 + 28n + 20 | u64       | MSB of tile's UUID      |
         * | 20 + 28n + 28 | ...       | &lt;next tile&gt;       |
         *
         * Due to nature of tile-based system, the size of layer chunk is aligned to multiple of
         * 1024 bytes. This is because tiles are frequently added to the file while drawing in empty
         * area. That means if user draw in 1500x1500 square area, the layer won't have to
         * reallocate when it ran out of space.
         *
         * However, if the canvas is sized, the implementation may choose to determine the maximum
         * number of tiles a layer can hold, then calculate the maximum number of bytes needed to
         * allocate layer chunk, thus effectively preventing reallocation.
         */
        const val CHUNK_TYPE_LAYER = 0x0003

        /**
         * ID for tile chunk.
         *
         * Each tile is encoded as follows:
         *
         * | Offset | Data type | Name                           |
         * | ------ | --------- | ------------------------------ |
         * | 0      | u64       | LSB of tile's UUID             |
         * | 8      | u64       | MSB of tile's UUID             |
         * | 16     | u8        | Compression type               |
         * | 17     | u24       | Data size                      |
         * | 20     | ...       | Chunk data (bitmap/compressed) |
         *
         * The chunk size of a tile have alignment of 64 bytes with recommended minimum
         * preallocation size of 1024 bytes. In case the compression type is uncompressed, the size
         * of the tile will always be fixed.
         *
         * Nahara's Sketchpad Document supports the following compression types:
         *
         * | Type ID | Name         | Description                      |
         * | ------- | ------------ | -------------------------------- |
         * | 0       | Uncompressed | Raw RGBA8888 data                |
         * | 1       | Deflate      | Deflate-compressed               |
         */
        const val CHUNK_TYPE_TILE = 0x0004

        fun init(
            channel: SeekableByteChannel,
            tileSizeLog: Int,
            size: Size,
            background: Color
        ): SketchpadDocumentV1 {
            val container = ContainerDocument.init(channel, NAMESPACE.toByteArray(Charsets.UTF_8))

            container.allocateWithJson(CHUNK_TYPE_METADATA, Metadata(
                tileSizeLog = tileSizeLog,
                size = size,
                background = background
            ))

            container.allocateWithJson(CHUNK_TYPE_LAYER_STACK, LayerStack(
                layers = emptyList(),
                active = null
            ))

            return SketchpadDocumentV1(container)
        }

        fun load(channel: SeekableByteChannel): SketchpadDocumentV1 {
            return SketchpadDocumentV1(ContainerDocument.load(channel))
        }
    }
}