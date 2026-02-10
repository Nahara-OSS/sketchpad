package io.github.naharaoss.skpd.utils

interface Color {
    data class Rgb(
        val r: Float,
        val g: Float,
        val b: Float
    ) : Color {
        override fun toRgb(): Rgb = this
    }

    fun toRgb(): Rgb
}
