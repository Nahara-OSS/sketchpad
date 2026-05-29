package io.github.naharaoss.skpd.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BlendMode {
    /**
     * Source over (with premultipled alpha).
     */
    @SerialName("source-over")
    SourceOver,
}