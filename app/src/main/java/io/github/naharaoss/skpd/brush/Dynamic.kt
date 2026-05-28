package io.github.naharaoss.skpd.brush

import io.github.naharaoss.skpd.utils.Graph
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Brush dynamic settings.
 *
 * Basically controlling the brush parameter based on stylus sensors.
 */
@Serializable
data class Dynamic(val base: Float, val modifiers: List<Modifier> = emptyList()) {
    /**
     * Obtain parameter value for given input event.
     *
     * @param [input] Stylus input event
     */
    fun forInput(input: StylusInput): Float {
        var result = base
        for (modifier in modifiers) result = modifier.forInput(input, result)
        return result
    }

    @Serializable
    data class Modifier(
        val id: String,
        val sensor: Sensor,
        val operation: Operation,
        val graph: Graph
    ) {
        fun forInput(input: StylusInput, base: Float): Float = operation.apply(base, graph(sensor.forInput(input)))
    }

    @Serializable
    sealed interface Operation {
        /**
         * Apply modifier operation.
         *
         * @param [base] The input value to be modified
         * @param [graph] The value obtained from the graph
         */
        fun apply(base: Float, graph: Float): Float

        /**
         * Add some amount on top of input value
         */
        @Serializable
        @SerialName("additive")
        data class Additive(val value: Float) : Operation {
            override fun apply(base: Float, graph: Float) = base + (value * graph)
        }

        /**
         * Multiply input value with some value
         */
        @Serializable
        @SerialName("multiplicative")
        data class Multiplicative(val gain: Float) : Operation {
            override fun apply(base: Float, graph: Float) = base * (gain * graph)
        }
    }
}