package io.github.naharaoss.skpd.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer

@Serializable
sealed interface Size {
    /**
     * Infinite size.
     *
     * This is used by Sketchpad documents to indicate the size of the canvas is infinite, allowing
     * user to draw pretty much anywhere.
     */
    @Serializable
    @SerialName("infinite")
    object Infinite : Size

    /**
     * Limited size.
     *
     * This is used by Sketchpad documents to indicate the size of the canvas is limited to a
     * rectangular bounds.
     */
    @Serializable
    @SerialName("sized")
    data class Sized(val width: Int, val height: Int) : Size {
        init {
            // We could just use UInt...
            // But JVM doesn't play well with it
            if (width < 0) throw IllegalArgumentException("Width must be positive")
            if (height < 0) throw IllegalArgumentException("Height must be positive")
        }
    }
}

fun ByteBuffer.putSize(size: Size): ByteBuffer = when (size) {
    is Size.Infinite -> {
        put(0)
    }
    is Size.Sized -> {
        put(1)
        putInt(size.width)
        putInt(size.height)
    }
}

fun ByteBuffer.getSize(): Size = when (get().toInt()) {
    0 -> Size.Infinite
    1 -> Size.Sized(getInt(), getInt())
    else -> throw IllegalArgumentException("Invalid size type")
}