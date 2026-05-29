package io.github.naharaoss.skpd.brush.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.utils.GraphEditor
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StampPresetEditor(
    modifier: Modifier = Modifier,
    preset: StampBrush.Preset,
    onPresetChange: (StampBrush.Preset) -> Unit,
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
                            onPresetChange(preset.copy(tip = when (preset.tip) {
                                is StampBrush.Preset.BrushTip.Simple -> StampBrush.Preset.BrushTip.Circle(
                                    falloff = preset.tip.falloff,
                                    scaleX = preset.tip.scaleX,
                                    scaleY = preset.tip.scaleY
                                )
                            }))
                            onPresetChangeFinished()
                        },
                        content = { Text("Circle") }
                    )

                    ToggleButton(
                        modifier = Modifier.width(80.dp),
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        checked = preset.tip is StampBrush.Preset.BrushTip.Square,
                        onCheckedChange = {
                            onPresetChange(preset.copy(tip = when (preset.tip) {
                                is StampBrush.Preset.BrushTip.Simple -> StampBrush.Preset.BrushTip.Square(
                                    falloff = preset.tip.falloff,
                                    scaleX = preset.tip.scaleX,
                                    scaleY = preset.tip.scaleY
                                )
                            }))
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
                        var decoupledGraph by remember(preset.tip.javaClass) { mutableStateOf(preset.tip.falloff) }

                        GraphEditor(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            enabled = true,
                            xAxisLabel = { Text("Distance") },
                            yAxisLabel = { Text("Opacity") },
                            graph = decoupledGraph,
                            onGraphChange = {
                                decoupledGraph = it
                                onPresetChange(preset.copy(tip = preset.tip.copyToSimple(falloff = it)))
                                onPresetChangeFinished()
                            }
                        )
                    }
                }
            }
        }

        BrushSliderParameter(
            icon = { Icon(painterResource(R.drawable.edit_24px), "Spacing") },
            label = { Text("Spacing") },
            formatValue = {
                when {
                    preset.spacing > 0 -> Text("${if (it < 10) "%.2f".format(it) else it.roundToInt()} pixels")
                    preset.spacing < 0 -> Text("${(it * 100).roundToInt()}%")
                }
            },
            value = abs(preset.spacing),
            range = when {
                preset.spacing > 0 -> 0.1f..1000f
                preset.spacing < 0 -> 0.01f..10f
                else -> 0.01f..1000f
            },
            forwardMapping = when {
                preset.spacing > 0 -> ({ (it / 1000f).pow(0.1f) })
                else -> ({ it })
            },
            backwardMapping = when {
                preset.spacing > 0 -> ({ it.pow(1f / 0.1f) * 1000f })
                else -> ({ it })
            },
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    ToggleButton(
                        modifier = Modifier.width(80.dp),
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        checked = preset.spacing > 0f,
                        onCheckedChange = {
                            if (preset.spacing < 0f) {
                                onPresetChange(preset.copy(spacing = 1f))
                                onPresetChangeFinished()
                            }
                        },
                        content = { Text("Fixed") }
                    )

                    ToggleButton(
                        modifier = Modifier.width(80.dp),
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        checked = preset.spacing < 0f,
                        onCheckedChange = {
                            if (preset.spacing > 0f) {
                                onPresetChange(preset.copy(spacing = -0.5f))
                                onPresetChangeFinished()
                            }
                        },
                        content = { Text("Auto") }
                    )
                }
            },
            onValueChange = {
                onPresetChange(preset.copy(spacing = when {
                    preset.spacing > 0 -> it
                    preset.spacing < 0 -> -it
                    else -> it
                }))
            },
            onValueChangeFinished = onPresetChangeFinished
        )

        StampBrush.allParameters.forEach { parameter ->
            val name = stringResource(parameter.nameRes)
            val dynamic = parameter.getDynamic(preset)

            BrushSliderParameter(
                icon = { Icon(painterResource(parameter.iconRes), name) },
                label = { Text(name) },
                formatValue = { Text(parameter.formatValue(it)) },
                value = dynamic.base,
                range = parameter.min..parameter.max,
                forwardMapping = parameter::forwardMapToSlider,
                backwardMapping = parameter::backwardMapToSlider,
                sliderTrack = when (parameter.centered) {
                    true -> { sliderState -> SliderDefaults.CenteredTrack(colors = SliderDefaults.colors(), sliderState = sliderState) }
                    false -> { sliderState -> SliderDefaults.Track(colors = SliderDefaults.colors(), sliderState = sliderState) }
                },
                action = {
                    ToggleButton(
                        checked = dynamic.modifiers.isNotEmpty(),
                        onCheckedChange = { onDynamicEditor(parameter.parameter) }
                    ) {
                        Icon(painterResource(R.drawable.edit_24px), "Sensor")
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Sensor")
                    }
                },
                onValueChange = {
                    val dynamic = dynamic.copy(base = it)
                    onPresetChange(parameter.replaceDynamic(preset, dynamic))
                },
                onValueChangeFinished = onPresetChangeFinished
            )
        }
    }
}