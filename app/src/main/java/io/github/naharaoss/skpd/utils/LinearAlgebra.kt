package io.github.naharaoss.skpd.utils

import android.view.Surface
import androidx.compose.ui.graphics.Matrix
import kotlinx.serialization.Serializable

@Serializable
data class Vector4(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    infix fun dot(v: Vector4) = x * v.x + y * v.y + z * v.z + w * v.w
}

@Serializable
data class Matrix4(
    val m00: Float, val m01: Float, val m02: Float, val m03: Float,
    val m10: Float, val m11: Float, val m12: Float, val m13: Float,
    val m20: Float, val m21: Float, val m22: Float, val m23: Float,
    val m30: Float, val m31: Float, val m32: Float, val m33: Float,
) {
    fun getRow(row: Int) = when (row) {
        0 -> Vector4(m00, m01, m02, m03)
        1 -> Vector4(m10, m11, m12, m13)
        2 -> Vector4(m20, m21, m22, m23)
        3 -> Vector4(m30, m31, m32, m33)
        else -> throw Exception("Invalid row number: $row")
    }

    fun getColumn(col: Int) = when (col) {
        0 -> Vector4(m00, m10, m20, m30)
        1 -> Vector4(m01, m11, m21, m31)
        2 -> Vector4(m02, m12, m22, m32)
        3 -> Vector4(m03, m13, m23, m33)
        else -> throw Exception("Invalid column number: $col")
    }

    fun toFloatArray() = floatArrayOf(
        m00, m01, m02, m03,
        m10, m11, m12, m13,
        m20, m21, m22, m23,
        m30, m31, m32, m33,
    )

    fun asAndroidx() = Matrix(toFloatArray())

    fun transpose() = Matrix4(
        m00, m10, m20, m30,
        m01, m11, m21, m31,
        m02, m12, m22, m32,
        m03, m13, m23, m33,
    )

    fun invert() = fromAndroidx(asAndroidx().apply { invert() })

    infix operator fun times(m: Matrix4) = Matrix4(
        getRow(0) dot m.getColumn(0), getRow(0) dot m.getColumn(1), getRow(0) dot m.getColumn(2), getRow(0) dot m.getColumn(3),
        getRow(1) dot m.getColumn(0), getRow(1) dot m.getColumn(1), getRow(1) dot m.getColumn(2), getRow(1) dot m.getColumn(3),
        getRow(2) dot m.getColumn(0), getRow(2) dot m.getColumn(1), getRow(2) dot m.getColumn(2), getRow(2) dot m.getColumn(3),
        getRow(3) dot m.getColumn(0), getRow(3) dot m.getColumn(1), getRow(3) dot m.getColumn(2), getRow(3) dot m.getColumn(3),
    )

    companion object {
        val Identity = Matrix4(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        val Rotate90 = Identity.copy(m00 = 0f, m01 = 1f, m10 = -1f, m11 = 0f)
        val Rotate180 = Identity.copy(m00 = -1f, m01 = 0f, m10 = 0f, m11 = -1f)
        val Rotate270 = Identity.copy(m00 = 0f, m01 = -1f, m10 = 1f, m11 = 0f)

        /**
         * Obtain surface orientation as matrix.
         */
        fun fromSurfaceOrientation(v: Int) = when (v) {
            Surface.ROTATION_90 -> Rotate90
            Surface.ROTATION_180 -> Rotate180
            Surface.ROTATION_270 -> Rotate270
            else -> Identity
        }

        fun fromAndroidx(m: Matrix) = Matrix4(
            m.values[0], m.values[1], m.values[2], m.values[3],
            m.values[4], m.values[5], m.values[6], m.values[7],
            m.values[8], m.values[9], m.values[10], m.values[11],
            m.values[12], m.values[13], m.values[14], m.values[15],
        )
    }
}