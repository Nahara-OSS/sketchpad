package io.github.naharaoss.skpd.utils

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

@Serializable
private data class SerializableColor(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float
) {
    fun toAndroidColor() = Color(red, green, blue, alpha)
}

private fun Color.toSerializableColor() = SerializableColor(red, green, blue, alpha)

object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = SerialDescriptor("androidx.compose.ui.graphics.Color", SerializableColor.serializer().descriptor)
    override fun serialize(encoder: Encoder, value: Color) = SerializableColor.serializer().serialize(encoder, value.toSerializableColor())
    override fun deserialize(decoder: Decoder) = SerializableColor.serializer().deserialize(decoder).toAndroidColor()
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor get() = PrimitiveSerialDescriptor(UUIDSerializer::class.qualifiedName ?: "UUIDSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) { encoder.encodeString(value.toString()) }
    override fun deserialize(decoder: Decoder) = UUID.fromString(decoder.decodeString())!!
}