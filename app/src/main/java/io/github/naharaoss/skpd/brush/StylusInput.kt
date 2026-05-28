package io.github.naharaoss.skpd.brush

import android.view.MotionEvent
import androidx.compose.ui.util.lerp
import kotlin.math.sqrt

/**
 * Record for stylus input events.
 */
data class StylusInput(
    /**
     * Timestamp of this input event.
     *
     * The timestamp is measured since the first event of the stroke. The time unit is in seconds.
     */
    val time: Float,

    /**
     * X position of the stylus.
     *
     * The position of this input event is in canvas coordinates, where `(0; 0)` corresponding to
     * the center of the canvas. This value corresponds to [android.view.MotionEvent.AXIS_X].
     */
    val x: Float,

    /**
     * Y position of the stylus.
     *
     * The position of this input event is in canvas coordinates, where `(0; 0)` corresponding to
     * the center of the canvas. This value corresponds to [android.view.MotionEvent.AXIS_Y].
     */
    val y: Float,

    /**
     * Velocity of the stylus.
     *
     * The velocity of the stylus is measured in pixels per second (px/s). Nahara's Sketchpad stylus
     * input stack might perform some smoothing on velocity value so that it can be stable for use
     * in brush dynamic.
     */
    val velocity: Float,

    /**
     * Normalized logical pressure of the stylus.
     *
     * A normalized logical pressure is a value that is scaled from the raw value to `[0; 1]` range.
     * In many cases, the shape of `y = f(x)` (where `y` is logical and `x` is physical) is not a
     * linear curve, but instead following the `x^(1/c)` form. This value corresponds to
     * [android.view.MotionEvent.AXIS_PRESSURE].
     */
    val pressure: Float,

    /**
     * Stylus altitude tilt angle.
     *
     * The value is 0 for completely flat on surface, and 90 for completely perpendicular to the
     * surface. This value corresponds to [android.view.MotionEvent.AXIS_TILT].
     */
    val altitude: Float,

    /**
     * Stylus azimuth tilt angle.
     *
     * The value is 0 for tilting to the right (+X direction), 90 for +Y, 180 for -X and 270 for -Y.
     * This value corresponds to [android.view.MotionEvent.AXIS_ORIENTATION].
     */
    val azimuth: Float,

    /**
     * Stylus barrel rotation.
     *
     * This is the barrel rotation of the stylus in degrees, from 0deg to 360deg. At current moment,
     * there is no known [android.view.MotionEvent] axis for stylus barrel rotation, but there is
     * at least 1 device with barrel rotation (Movinkpad Pro 14 + Art Pen 2). Further testing is
     * required to determine the correct axis.
     */
    val rotation: Float,
) {
    infix fun distanceTo(input: StylusInput): Float {
        val dx = input.x - x
        val dy = input.y - y
        return sqrt(dx * dx + dy * dy)
    }

    companion object {
        fun fromMotionEvent(prev: StylusInput?, next: MotionEvent): StylusInput {
            val x = next.x
            val y = next.y
            val dx = if (prev != null) x - prev.x else 0f
            val dy = if (prev != null) y - prev.y else 0f
            val time = (next.eventTime - next.downTime) / 1000f
            val delta = if (prev != null) time - prev.time else 0f
            val vx = if (prev != null) dx / delta else 0f
            val vy = if (prev != null) dy / delta else 0f
            val velocity = sqrt(vx * vx + vy * vy)
            val pressure = next.getAxisValue(MotionEvent.AXIS_PRESSURE)
            val altitude = next.getAxisValue(MotionEvent.AXIS_TILT)
            val azimuth = next.getAxisValue(MotionEvent.AXIS_ORIENTATION)
            val rotation = 0f
            return StylusInput(time, x, y, velocity, pressure, altitude, azimuth, rotation)
        }
    }
}

fun lerp(a: StylusInput, b: StylusInput, fraction: Float) = StylusInput(
    time = lerp(a.time, b.time, fraction),
    x = lerp(a.x, b.x, fraction),
    y = lerp(a.y, b.y, fraction),
    velocity = lerp(a.velocity, b.velocity, fraction),
    pressure = lerp(a.pressure, b.pressure, fraction),
    altitude = lerp(a.altitude, b.altitude, fraction),
    azimuth = lerp(a.azimuth, b.azimuth, fraction), // FIXME: Circle lerp
    rotation = lerp(a.rotation, b.rotation, fraction), // FIXME: Circle lerp
)