package io.github.naharaoss.skpd.engine.brush

import android.view.MotionEvent
import androidx.compose.ui.util.lerp
import kotlin.math.pow
import kotlin.math.sqrt

data class StylusInput(
    /**
     * Timestamp of this stylus input since the start of the stroke. Measured in seconds.
     */
    val time: Float,

    val x: Float,
    val y: Float,

    /**
     * The logical pressure of the stylus (after applied app-level pressure curve).
     */
    val pressure: Float,

    /**
     * Azimuth of the stylus (also known as "tilt direction").
     */
    val azimuth: Float,

    /**
     * Tilt altitude of the stylus. The value is 0 when the stylus is perpendicular to the surface.
     */
    val altitude: Float,

    /**
     * Barrel rotation of the stylus along its axis.
     */
    val rotation: Float,
) {
    constructor(event: MotionEvent) : this(
        time = event.eventTime / 1000f,
        x = event.getAxisValue(MotionEvent.AXIS_X),
        y = event.getAxisValue(MotionEvent.AXIS_Y),
        pressure = event.getAxisValue(MotionEvent.AXIS_PRESSURE),
        azimuth = event.getAxisValue(MotionEvent.AXIS_ORIENTATION),
        altitude = event.getAxisValue(MotionEvent.AXIS_TILT),
        rotation = 0f, // TODO
    )

    infix fun distanceTo(target: StylusInput): Float {
        val dx = target.x - x
        val dy = target.y - y
        return sqrt(dx * dx + dy * dy)
    }
}

fun lerp(start: StylusInput, stop: StylusInput, fraction: Float) = StylusInput(
    time = lerp(start.time, stop.time, fraction),
    x = lerp(start.x, stop.x, fraction),
    y = lerp(start.y, stop.y, fraction),
    pressure = lerp(start.pressure, stop.pressure, fraction),
    azimuth = lerp(start.azimuth, stop.azimuth, fraction), // TODO
    altitude = lerp(start.altitude, stop.altitude, fraction),
    rotation = lerp(start.rotation, stop.rotation, fraction), // TODO
)
