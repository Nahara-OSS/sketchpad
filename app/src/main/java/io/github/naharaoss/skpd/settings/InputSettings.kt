package io.github.naharaoss.skpd.settings

import io.github.naharaoss.skpd.utils.Graph
import kotlinx.serialization.Serializable

/**
 * Input settings.
 *
 * Configure and calibrate stylus device.
 */
@Serializable
data class InputSettings(
    /**
     * Finger drawing mode.
     *
     * Allow drawing with finger. Should be disabled by default on devices that have built-in
     * stylus. Devices with optional stylus support (where user have to buy stylus) should keep
     * finger drawing enabled.
     */
    val fingerDrawing: Boolean = true,

    /**
     * Pressure calibration graph.
     */
    val pressureGraph: Graph = Graph(),

    /**
     * Position smoothing.
     *
     * The position smoothing algorithm uses moving average algorithm, with the smoothing value
     * measured in seconds. The position is typically smoothed by driver/firmware before being
     * processed by the app, so this setting is pretty much unnecessary. User might want to increase
     * this if the input appears wobbly, especially on those USI-based tablets.
     */
    val positionSmoothing: Float = 0f,

    /**
     * Velocity smoothing.
     *
     * The velocity smoothing algorithm uses moving average algorithm, with the smoothing value
     * measured in seconds. The stability of velocity depend on the consistency of input events
     * being emitted to app.
     */
    val velocitySmoothing: Float = 0.6f,
)