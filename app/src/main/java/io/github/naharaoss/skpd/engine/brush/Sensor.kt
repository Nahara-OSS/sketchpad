package io.github.naharaoss.skpd.engine.brush

import io.github.naharaoss.skpd.R
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

sealed interface Sensor {
    val resourceId: Int?

    fun derive(prev: StylusInput?, next: StylusInput): Float

    object Pressure : Sensor {
        override val resourceId: Int get() = R.string.brush_sensor_pressure

        override fun derive(prev: StylusInput?, next: StylusInput): Float {
            return next.pressure
        }
    }

    object Jitter : Sensor {
        override val resourceId: Int get() = R.string.brush_sensor_jitter

        override fun derive(prev: StylusInput?, next: StylusInput): Float {
            return Random.nextFloat()
        }
    }

    data class Speed(
        /**
         * Maximum movement speed, which will be used to map movement speed to [0.00; 1.00] range.
         * Speed is measured in px/s.
         */
        val maxSpeed: Float
    ) : Sensor {
        override val resourceId: Int get() = R.string.brush_sensor_speed

        override fun derive(prev: StylusInput?, next: StylusInput): Float {
            if (prev == null) return 0f
            val distance = prev distanceTo next
            val duration = next.time - prev.time
            return min(max((distance / duration) / maxSpeed, 0f), 1f)
        }
    }
}