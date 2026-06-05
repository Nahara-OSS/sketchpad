package io.github.naharaoss.skpd.resource

import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.Dynamic
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.utils.Graph
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

@Serializable
private sealed interface SerializablePreset {
    @Serializable
    @SerialName("stamp")
    data class Stamp(
        val tip: BrushTip,
        val spacing: Float,
        val count: Dynamic,
        val size: Dynamic,
        val opacity: Dynamic,
        val flow: Dynamic,
        val rotation: Dynamic,
        val offsetX: Dynamic,
        val offsetY: Dynamic
    ) : SerializablePreset {
        @Serializable
        sealed interface BrushTip {
            @Serializable
            @SerialName("square")
            data class Square(val falloff: Graph, val scaleX: Float, val scaleY: Float) : BrushTip

            @Serializable
            @SerialName("circle")
            data class Circle(val falloff: Graph, val scaleX: Float, val scaleY: Float) : BrushTip
        }

        companion object {
            const val BRUSH_TIP_FILE_NAME = "brush-tip.png"
        }
    }

    companion object {
        const val PRESET_FILE_NAME = "brush-preset.json"
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun File.loadBrushPresetFromFolder(): BrushType.Preset {
    val presetFile = File(this, SerializablePreset.PRESET_FILE_NAME)
    val base: SerializablePreset = presetFile.inputStream().use { Json.decodeFromStream(it) }

    return when (base) {
        is SerializablePreset.Stamp -> loadStampBrushPreset(base)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun File.storeBrushPresetToFolder(preset: BrushType.Preset) {
    val base: SerializablePreset = when (preset) {
        is StampBrush.Preset -> storeStampBrushPreset(preset)
        else -> throw Exception("Not implemented for $preset")
    }

    val presetFile = File(this, SerializablePreset.PRESET_FILE_NAME)
    presetFile.outputStream().use { Json.encodeToStream(base, it) }
}

private fun File.loadStampBrushPreset(base: SerializablePreset.Stamp): StampBrush.Preset = StampBrush.Preset(
    tip = when (base.tip) {
        is SerializablePreset.Stamp.BrushTip.Square -> StampBrush.Preset.BrushTip.Square(base.tip.falloff, base.tip.scaleX, base.tip.scaleY)
        is SerializablePreset.Stamp.BrushTip.Circle -> StampBrush.Preset.BrushTip.Circle(base.tip.falloff, base.tip.scaleX, base.tip.scaleY)
    },
    spacing = base.spacing,
    count = base.count,
    size = base.size,
    opacity = base.opacity,
    flow = base.flow,
    rotation = base.rotation,
    offsetX = base.offsetX,
    offsetY = base.offsetY
)

private fun File.storeStampBrushPreset(preset: StampBrush.Preset): SerializablePreset.Stamp = SerializablePreset.Stamp(
    tip = when (preset.tip) {
        is StampBrush.Preset.BrushTip.Square -> SerializablePreset.Stamp.BrushTip.Square(preset.tip.falloff, preset.tip.scaleX, preset.tip.scaleY)
        is StampBrush.Preset.BrushTip.Circle -> SerializablePreset.Stamp.BrushTip.Circle(preset.tip.falloff, preset.tip.scaleX, preset.tip.scaleY)
    },
    spacing = preset.spacing,
    count = preset.count,
    size = preset.size,
    opacity = preset.opacity,
    flow = preset.flow,
    rotation = preset.rotation,
    offsetX = preset.offsetX,
    offsetY = preset.offsetY
)