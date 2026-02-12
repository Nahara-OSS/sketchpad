package io.github.naharaoss.skpd.engine.brush

import kotlinx.serialization.Serializable

@Serializable
data class BrushPreset(
    /**
     * The user-defined display name for this brush preset.
     */
    val name: String,

    /**
     * The tags associated with this brush preset. Tags are user-defined.
     */
    val tags: List<String>,

    /**
     * Settings for the brush in this preset. The "engine type" of the brush is determined by the
     * class type of the brush settings.
     */
    val settings: Brush
)
