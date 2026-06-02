package io.github.naharaoss.skpd.utils

import io.github.naharaoss.container.ByteChannelUtils
import io.github.naharaoss.container.ContainerDocument
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer

inline fun <reified T> ContainerDocument.decodeJson(data: ContainerDocument.Data): T {
    val buffer = ByteBuffer.wrap(ByteArray(data.size))
    buffer.clear()
    buffer.limit(data.size)
    channel.position(data.offset)
    ByteChannelUtils.readFully(channel, buffer)
    return Json.decodeFromString<T>(buffer.array().toString(Charsets.UTF_8))
}

inline fun <reified T> ContainerDocument.allocateWithJson(type: Int, value: T) {
    val buffer = ByteBuffer.wrap(Json.encodeToString(value).toByteArray(Charsets.UTF_8))
    val data = allocate(type, buffer.remaining())
    channel.position(data.offset)
    ByteChannelUtils.writeFully(channel, buffer)
}

fun Int.alignTo(alignment: Int): Int {
    val blocks = this / alignment
    val modulo = this % alignment
    return blocks * alignment + (if (modulo != 0) alignment else 0)
}