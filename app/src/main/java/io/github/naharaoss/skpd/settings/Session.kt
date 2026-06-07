package io.github.naharaoss.skpd.settings

import io.github.naharaoss.skpd.utils.Color
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val selectedBrushId: Long? = null,
    val selectedBrushColor: Color = Color.Rgb(0f, 0f, 0f)
)