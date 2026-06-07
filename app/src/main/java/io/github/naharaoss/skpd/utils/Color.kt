package io.github.naharaoss.skpd.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
sealed interface Color {
    fun toRgb(): Rgb

    fun toAndroidx(): androidx.compose.ui.graphics.Color {
        val (r, g, b) = toRgb()
        return androidx.compose.ui.graphics.Color(r, g, b, 1f)
    }

    @Serializable
    @SerialName("rgba")
    data class Rgb(val r: Float, val g: Float, val b: Float) : Color {
        override fun toRgb(): Rgb = this
    }

    @Serializable
    data class Hsl(val h: Float, val s: Float, val l: Float) : Color {
        override fun toRgb(): Rgb {
            val c = (1f - abs(2f * l - 1f)) * s
            val h = h * 6f
            val x = c * (1f - abs((h % 2f) - 1f))
            return when (h) {
                in 0f..<1f -> Rgb(c, x, 0f)
                in 1f..<2f -> Rgb(x, c, 0f)
                in 2f..<3f -> Rgb(0f, c, x)
                in 3f..<4f -> Rgb(0f, x, c)
                in 4f..<5f -> Rgb(x, 0f, c)
                in 5f..<6f -> Rgb(c, 0f, x)
                else -> Rgb(0f, 0f, 0f)
            }
        }
    }

    companion object {
        val Black = Rgb(0f, 0f, 0f)
        val White = Rgb(1f, 1f, 1f)
    }
}