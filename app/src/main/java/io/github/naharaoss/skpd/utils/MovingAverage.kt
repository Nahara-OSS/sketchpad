package io.github.naharaoss.skpd.utils

/**
 * Implementation of basic moving average algorithm.
 *
 * This algorithm is meant to be used for smoothing stylus velocity value. This is because on some
 * devices, the [android.view.MotionEvent] are not being emitted consistently, causing velocity to
 * go all over the place.
 */
class MovingAverage(val maxDuration: Float) {
    private val history = mutableListOf<Pair<Float, Float>>()
    private var duration = 0f

    /**
     * Consume and obtain smoothed value.
     *
     * @param [delta] The time span between last and next event
     * @param [value] The value obtained between last and next event
     */
    fun consume(delta: Float, value: Float): Float {
        if (delta > 0f) {
            history.add(delta to value)
            duration += delta

            var pendingRemoval = duration - maxDuration

            while (pendingRemoval > 0f) {
                if (pendingRemoval >= history[0].first) {
                    pendingRemoval -= history[0].first
                    duration -= history[0].first
                    history.removeAt(0)
                } else {
                    duration -= pendingRemoval
                    history[0] = history[0].copy(first = history[0].first - pendingRemoval)
                    break;
                }
            }
        }

        return history.sumOf { (it.first * it.second).toDouble() }.toFloat() / duration
    }

    /**
     * Clear history.
     */
    fun clear() {
        history.clear()
        duration = 0f
    }
}