package io.github.naharaoss.skpd.impl.brush.dab

import io.github.naharaoss.skpd.engine.brush.Brush
import io.github.naharaoss.skpd.engine.brush.Dynamic
import io.github.naharaoss.skpd.engine.brush.Sensor
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class DabBrush(
    /**
     * The shape of the brush. The shape produces a bitmap, which will then be used for stamping to
     * stroke layer.
     */
    val shape: Shape,

    /**
     * The size of the brush dab. If the dab shape is a circle, the size value is the diameter of
     * that circle.
     */
    val size: Dynamic,

    /**
     * The opacity of the stroke. Note that this is not the opacity of each individual dab, which is
     * controlled by [flow].
     */
    val opacity: Dynamic,

    /**
     * The opacity of each individual dab.
     */
    val flow: Dynamic,

    /**
     * The spacing for each individual brush dab. The smaller the spacing, the more processing the
     * brush renderer must do. For most optimal performance, the spacing should only increase based
     * on brush size, where bigger brush size have larger spacing.
     */
    val spacing: Dynamic,

    /**
     * Offset of the brush dab along X axis. May be used to adjust center position of the brush
     * shape.
     */
    val offsetX: Dynamic,

    /**
     * Offset of the brush dab along Y axis.
     */
    val offsetY: Dynamic,

    /**
     * Maximum scattering distance along X axis. May be used for making particles.
     */
    val scatterX: Dynamic,

    /**
     * Maximum scattering distance along Y axis.
     */
    val scatterY: Dynamic,
) : Brush {
    interface Shape {
        val bitmapWidth: Int
        val bitmapHeight: Int

        fun writeBitmap(dst: ByteBuffer)

        object Square : Shape {
            override val bitmapWidth: Int get() = 1
            override val bitmapHeight: Int get() = 1

            override fun writeBitmap(dst: ByteBuffer) {
                // RGBA
                dst.put(0).put(0).put(0).put(255.toByte())
            }
        }

        data class Circle(val hardness: Float) : Shape {
            override val bitmapWidth: Int get() = 64
            override val bitmapHeight: Int get() = 64

            override fun writeBitmap(dst: ByteBuffer) {
                for (y in 0..<bitmapHeight) {
                    for (x in 0..<bitmapWidth) {
                        val nx = x * 2f / (bitmapWidth - 1f) - 1f
                        val ny = y * 2f / (bitmapHeight - 1f) - 1f
                        val v = 1f - sqrt(nx * nx + ny * ny).pow(hardness)
                        dst.put(0).put(0).put(0)
                        dst.put((min(max(v, 0f), 1f) * 255f).toInt().toByte())
                    }
                }
            }
        }
    }

    companion object {
        val Default = DabBrush(
            shape = Shape.Circle(4f),
            size = Dynamic(
                base = 12f,
                modifiers = listOf(
                    Dynamic.Modifier(
                        sensor = Sensor.Pressure,
                        operation = Dynamic.Modifier.Operation.Multiply
                    )
                )
            ),
            opacity = Dynamic(base = 1f),
            flow = Dynamic(base = 1f),
            spacing = Dynamic(base = 1f),
            offsetX = Dynamic(base = 0f),
            offsetY = Dynamic(base = 0f),
            scatterX = Dynamic(base = 0f),
            scatterY = Dynamic(base = 0f),
        )

        val Pencil = DabBrush(
            shape = Shape.Circle(2f),
            size = Dynamic(
                base = 8f,
                modifiers = listOf(
                    Dynamic.Modifier(
                        sensor = Sensor.Pressure,
                        operation = Dynamic.Modifier.Operation.Multiply
                    )
                )
            ),
            opacity = Dynamic(
                base = 1f,
                modifiers = listOf(
                    Dynamic.Modifier(
                        sensor = Sensor.Pressure,
                        operation = Dynamic.Modifier.Operation.Multiply
                    )
                )
            ),
            flow = Dynamic(base = 0.3f),
            spacing = Dynamic(base = 1f),
            offsetX = Dynamic(base = 0f),
            offsetY = Dynamic(base = 0f),
            scatterX = Dynamic(base = 2f),
            scatterY = Dynamic(base = 2f),
        )
    }
}
