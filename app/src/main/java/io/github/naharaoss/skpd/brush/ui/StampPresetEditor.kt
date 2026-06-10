package io.github.naharaoss.skpd.brush.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.utils.GraphEditor
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StampPresetEditor(
    modifier: Modifier = Modifier,
    preset: StampBrush.Preset,
    onPresetChange: ((StampBrush.Preset) -> StampBrush.Preset) -> Unit,
    onPresetChangeFinished: () -> Unit,
    onDynamicEditor: (String) -> Unit
) {
    Column(modifier) {
        BrushParameterLayout(
            modifier = Modifier.padding(top = 8.dp),
            icon = { Icon(painterResource(R.drawable.edit_24px), "Shape") },
            label = { Text("Shape") },
            value = {
                when (preset.tip) {
                    is StampBrush.Preset.BrushTip.Circle -> Text("Circle")
                    is StampBrush.Preset.BrushTip.Square -> Text("Square")
                }
            },
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ToggleButton(
                        modifier = Modifier.width(80.dp),
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        checked = preset.tip is StampBrush.Preset.BrushTip.Circle,
                        onCheckedChange = {
                            onPresetChange { preset ->
                                preset.copy(tip = when (preset.tip) {
                                    is StampBrush.Preset.BrushTip.Simple -> StampBrush.Preset.BrushTip.Circle(
                                        falloff = preset.tip.falloff,
                                        scaleX = preset.tip.scaleX,
                                        scaleY = preset.tip.scaleY
                                    )
                                })
                            }
                            onPresetChangeFinished()
                        },
                        content = { Text("Circle") }
                    )

                    ToggleButton(
                        modifier = Modifier.width(80.dp),
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        checked = preset.tip is StampBrush.Preset.BrushTip.Square,
                        onCheckedChange = {
                            onPresetChange { preset ->
                                preset.copy(tip = when (preset.tip) {
                                    is StampBrush.Preset.BrushTip.Simple -> StampBrush.Preset.BrushTip.Square(
                                        falloff = preset.tip.falloff,
                                        scaleX = preset.tip.scaleX,
                                        scaleY = preset.tip.scaleY
                                    )
                                })
                            }
                            onPresetChangeFinished()
                        },
                        content = { Text("Square") }
                    )
                }
            }
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ) {
                when (preset.tip) {
                    is StampBrush.Preset.BrushTip.Simple -> {
                        GraphEditor(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            enabled = true,
                            xAxisLabel = { Text("Distance") },
                            yAxisLabel = { Text("Opacity") },
                            graph = preset.tip.falloff,
                            onGraphChange = { graph ->
                                onPresetChange { preset -> if (preset.tip is StampBrush.Preset.BrushTip.Simple) preset.copy(tip = preset.tip.copyToSimple(falloff = graph)) else preset }
                                onPresetChangeFinished()
                            }
                        )
                    }
                }
            }
        }

        BrushSliderParameter(
            icon = { Icon(painterResource(R.drawable.animation_24px), "Spacing") },
            label = { Text("Spacing") },
            formatValue = { Text(if (preset.spacing >= 0) "${if (it < 10) "%.2f".format(it) else it.roundToInt()} pixels" else "${(it * 100).roundToInt()}%") },
            value = abs(preset.spacing),
            valueRange = if (preset.spacing >= 0) 0.1f..1000f else 0.01f..10f,
            exponent = if (preset.spacing >= 0) 0.5f else 1f,
            onValueChange = { onPresetChange { preset -> preset.copy(spacing = if (preset.spacing >= 0) it else -it) } },
            onValueChangeFinished = onPresetChangeFinished,
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ToggleButton(
                        modifier = Modifier.width(80.dp),
                        checked = preset.spacing >= 0,
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        onCheckedChange = {
                            if (it) onPresetChange { preset -> preset.copy(spacing = preset.size.base * -preset.spacing) }
                            onPresetChangeFinished()
                        },
                        content = { Text("Fixed") }
                    )
                    ToggleButton(
                        modifier = Modifier.width(80.dp),
                        checked = preset.spacing < 0,
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        onCheckedChange = {
                            if (it) onPresetChange { preset -> preset.copy(spacing = -(preset.spacing / preset.size.base)) }
                            onPresetChangeFinished()
                        },
                        content = { Text("Auto") }
                    )
                }
            }
        )

        for (parameter in StampBrush.allParameters) {
            val dynamic = parameter.getDynamic(preset)
            val name = stringResource(parameter.nameRes)

            BrushSliderParameter(
                icon = { Icon(painterResource(parameter.iconRes), name) },
                label = { Text(name) },
                formatValue = { Text(parameter.formatValue(it)) },
                value = dynamic.base,
                valueRange = parameter.valueRange,
                exponent = parameter.exponent,
                onValueChange = { onPresetChange { preset -> parameter.replaceDynamic(preset, dynamic.copy(base = it)) } },
                onValueChangeFinished = onPresetChangeFinished,
                action = {
                    ToggleButton(
                        checked = dynamic.modifiers.isNotEmpty(),
                        onCheckedChange = { onDynamicEditor(parameter.parameter) }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing)) {
                            Icon(painterResource(R.drawable.edit_24px), "Sensor")
                            Text("Sensor")
                        }
                    }
                }
            )
        }
    }
}