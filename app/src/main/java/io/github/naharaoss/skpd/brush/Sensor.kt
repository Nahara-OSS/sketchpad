package io.github.naharaoss.skpd.brush

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.naharaoss.skpd.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Serializable
sealed interface Sensor {
    @get:StringRes val nameRes: Int
    @get:DrawableRes val iconRes: Int

    fun forInput(input: StylusInput): Float

    companion object {
        val AllDefaults = listOf(
            Pressure,
            Altitude,
            Azimuth,
            Rotation,
            TiltX,
            TiltY,
            Velocity(1000f),
            Time(1f),
            StrokeJitter,
            DabJitter
        )
    }

    /**
     * Logical pressure normalized to 0 -> 1.
     */
    @Serializable
    @SerialName("pressure")
    object Pressure : Sensor {
        override val nameRes: Int = R.string.sensor_pressure
        override val iconRes: Int = R.drawable.compress_24px
        override fun forInput(input: StylusInput): Float = input.pressure
    }

    /**
     * Tilt altitude normalized from 0 (flat on surface) to 1 (perpendicular to canvas).
     */
    @Serializable
    @SerialName("altitude")
    object Altitude : Sensor {
        override val nameRes: Int = R.string.sensor_altitude
        override val iconRes: Int = R.drawable.height_24px
        override fun forInput(input: StylusInput): Float = input.altitude / 90f
    }

    /**
     * Tilt azimuth (tilt direction), scaled from 0 -> 360 range to 0 -> 1.
     */
    @Serializable
    @SerialName("azimuth")
    object Azimuth : Sensor {
        override val nameRes: Int = R.string.sensor_azimuth
        override val iconRes: Int = R.drawable.rotate_left_24px
        override fun forInput(input: StylusInput): Float = input.azimuth / 360f
    }

    /**
     * Barrel rotation, scaled from 0 -> 360 range to 0 -> 1.
     */
    @Serializable
    @SerialName("rotation")
    object Rotation : Sensor {
        override val nameRes: Int = R.string.sensor_rotation
        override val iconRes: Int = R.drawable.rotate_left_24px
        override fun forInput(input: StylusInput): Float = input.rotation
    }

    /**
     * Tilt amount from left to right, scaled from -90 -> +90 to 0 -> 1.
     */
    @Serializable
    @SerialName("tiltX")
    object TiltX : Sensor {
        override val nameRes: Int = R.string.sensor_tilt_x
        override val iconRes: Int = R.drawable.x_circle_24px

        override fun forInput(input: StylusInput): Float {
            val azimuth = input.azimuth * PI.toFloat() / 180f
            val altitude = input.altitude * PI.toFloat() / 180f
            return -sin(azimuth) * cos(altitude)
        }
    }

    /**
     * Tilt amount from top to bottom, scaled from -90 -> +90 to 0 -> 1.
     */
    @Serializable
    @SerialName("tiltY")
    object TiltY : Sensor {
        override val nameRes: Int = R.string.sensor_tilt_y
        override val iconRes: Int = R.drawable.y_circle_24px

        override fun forInput(input: StylusInput): Float {
            val azimuth = input.azimuth * PI.toFloat() / 180f
            val altitude = input.altitude * PI.toFloat() / 180f
            return cos(azimuth) * cos(altitude)
        }
    }

    /**
     * Stylus movement speed (velocity). Max value in px/s.
     */
    @Serializable
    @SerialName("velocity")
    data class Velocity(val max: Float) : Sensor {
        override val nameRes: Int = R.string.sensor_velocity
        override val iconRes: Int = R.drawable.speed_24px
        override fun forInput(input: StylusInput): Float = min(input.velocity / max, 1f)
    }

    /**
     * Progressed time since start of the stroke. Max value in seconds.
     */
    @Serializable
    @SerialName("time")
    data class Time(val max: Float) : Sensor {
        override val nameRes: Int = R.string.sensor_time
        override val iconRes: Int = R.drawable.timer_24px
        override fun forInput(input: StylusInput): Float = min(input.time / max, 1f)
    }

    /**
     * Random value from 0 to 1 per stroke.
     */
    @Serializable
    @SerialName("strokeJitter")
    object StrokeJitter : Sensor {
        override val nameRes: Int = R.string.sensor_stroke_jitter
        override val iconRes: Int = R.drawable.icon_123_24px
        override fun forInput(input: StylusInput): Float = input.strokeJitter
    }

    /**
     * Random value from 0 to 1 per stylus event.
     */
    @Serializable
    @SerialName("dabJitter")
    object DabJitter : Sensor {
        override val nameRes: Int = R.string.sensor_dab_jitter
        override val iconRes: Int = R.drawable.icon_123_24px
        override fun forInput(input: StylusInput): Float = Random.nextFloat()
    }
}