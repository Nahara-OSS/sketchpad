package io.github.naharaoss.skpd.toolbar

import io.github.naharaoss.skpd.utils.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Toolbar(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val side: Side,
    val position: Position,
    val docked: Boolean,
    val tools: List<Tool>
) {
    @Serializable
    enum class Side(val orientation: Orientation) {
        @SerialName("top") Top(Orientation.Horizontal),
        @SerialName("bottom") Bottom(Orientation.Horizontal),
        @SerialName("left") Left(Orientation.Vertical),
        @SerialName("right") Right(Orientation.Vertical)
    }

    @Serializable
    enum class Position {
        @SerialName("start") Start,
        @SerialName("center") Center,
        @SerialName("end") End
    }

    enum class Orientation {
        Vertical,
        Horizontal;

        fun rotate() = when (this) {
            Vertical -> Horizontal
            Horizontal -> Vertical
        }
    }
}