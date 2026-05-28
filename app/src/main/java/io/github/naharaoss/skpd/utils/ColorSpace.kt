package io.github.naharaoss.skpd.utils

import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

/**
 * Color space in Nahara's Sketchpad.
 *
 * Nahara's Sketchpad only support RGB color model, as to keep everything as simple as possible. In
 * RGB model, there are various color spaces, with sRGB being the most common (and recommended for
 * digital media).
 *
 * Currently, only sRGB is supported by Nahara's Sketchpad. Future version may introduce DCI-P3 due
 * to growing adoption.
 */
@Serializable
sealed interface ColorSpace {
    @Serializable
    enum class Standard : ColorSpace {
        SRGB
    }
}

fun ByteBuffer.putColorSpace(colorSpace: ColorSpace): ByteBuffer = when (colorSpace) {
    is ColorSpace.Standard -> {
        put(0)
        putInt(colorSpace.ordinal)
    }
}

fun ByteBuffer.getColorSpace(): ColorSpace = when (get().toInt()) {
    0 -> ColorSpace.Standard.entries[get().toInt()]
    else -> throw IllegalArgumentException("Invalid color space type")
}
