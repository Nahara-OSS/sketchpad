package io.github.naharaoss.skpd.resource

import kotlinx.serialization.Serializable

@Serializable
data class BrushItem(
    val id: Long,
    val name: String,
    val icon: String?
)