package io.github.naharaoss.skpd.engine.brush

/**
 * Brush dynamic controls the brush parameter based on sensor data (eg: map pressure to brush size
 * or opacity).
 */
data class Dynamic(val base: Float, val modifiers: List<Modifier> = emptyList()) {
    data class Modifier(
        val sensor: Sensor,
        val operation: Operation
    ) {
        enum class Operation(val apply: (x: Float, y: Float) -> Float) {
            Add({ x, y -> x + y }),
            Subtract({ x, y -> x - y }),
            Multiply({ x, y -> x * y }),
            Divide({ x, y -> x / y }),
        }

        fun apply(value: Float, prev: StylusInput?, next: StylusInput): Float {
            return operation.apply(value, sensor.derive(prev, next))
        }
    }

    /**
     * Apply brush dynamic.
     *
     * @param prev Previous stylus input event. Use `null` if apply the dynamic on the first event
     * of the stroke.
     * @param next Next stylus input event.
     */
    fun apply(prev: StylusInput?, next: StylusInput): Float {
        var value = base
        for (modifier in modifiers) value = modifier.apply(value, prev, next)
        return value
    }
}
