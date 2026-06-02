package io.github.naharaoss.skpd.utils

import java.nio.ByteBuffer
import java.util.UUID
import kotlin.experimental.and

fun ByteBuffer.putIntWithMax(value: Int, max: Int): ByteBuffer = when {
    max <= 0x7F -> put(value.toByte())
    max <= 0x7FFF -> putShort(value.toShort())
    else -> putInt(value)
}

fun ByteBuffer.getIntWithMax(max: Int): Int = when {
    max <= 0x7F -> get().toInt()
    max <= 0x7FFF -> getShort().toInt()
    else -> getInt()
}

fun ByteBuffer.putLpByteArray(arr: ByteArray, max: Int): ByteBuffer {
    if (arr.size > max) throw IllegalArgumentException("Array too big (expecting ${arr.size} <= $max)")
    return putIntWithMax(arr.size, max).put(arr)
}

fun ByteBuffer.getLpByteArray(max: Int): ByteArray {
    val size = getIntWithMax(max)
    if (size >= max) throw IllegalArgumentException("Size too big (expecting $size <= $max)")
    return ByteArray(max).also { get(it) }
}

fun ByteBuffer.putUUID(uuid: UUID): ByteBuffer = this
    .putLong(uuid.leastSignificantBits)
    .putLong(uuid.mostSignificantBits)

fun ByteBuffer.getUUID(): UUID {
    val lsb = getLong()
    val msb = getLong()
    return UUID(msb, lsb)
}

fun ByteBuffer.putInt24(value: Int): ByteBuffer = this
    .put((value and 0x0000FF).toByte())
    .putShort(((value and 0xFFFF00) shr 8).toShort())

fun ByteBuffer.getInt24(): Int {
    val lsb = get().toInt() and 0xFF
    val msb = (getShort().toInt() and 0xFFFF) shl 8
    return lsb or msb
}