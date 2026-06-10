package io.github.naharaoss.skpd.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.naharaoss.skpd.ui.theme.SketchpadTheme
import kotlin.math.pow

@Composable
fun SketchpadSlider(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    exponent: Float = 1f,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
) {
    val mappedValue = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val mappedRange = 0f..1f

    Slider(
        modifier = modifier,
        enabled = enabled,
        value = mappedValue.forwardMap(exponent),
        valueRange = mappedRange,
        onValueChange = { value ->
            val fraction = value.backwardMap(exponent)
            val value = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
            onValueChange(value)
        },
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, colors = colors, enabled = enabled) }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SketchpadCenteredSlider(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    exponent: Float = 1f,
    value: Float,
    valueRange: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    val mappedValue = value / valueRange
    val mappedRange = -1f..1f

    Slider(
        modifier = modifier,
        enabled = enabled,
        value = mappedValue.forwardMap(exponent),
        valueRange = mappedRange,
        onValueChange = { value ->
            val fraction = value.backwardMap(exponent)
            val value = fraction * valueRange
            onValueChange(value)
        },
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        track = { sliderState -> SliderDefaults.CenteredTrack(sliderState = sliderState, colors = colors, enabled = enabled) }
    )
}

@Composable
fun SketchpadRangeSlider(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    exponent: Float = 1f,
    value: ClosedFloatingPointRange<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    val centered = -valueRange.start == valueRange.endInclusive
    val travelAmount = valueRange.endInclusive - valueRange.start
    val mappedValue = when (centered) {
        true -> (value.start / valueRange.endInclusive)..(value.endInclusive / valueRange.endInclusive)
        false -> ((value.start - valueRange.start) / travelAmount)..((value.endInclusive - valueRange.start) / travelAmount)
    }
    val mappedRange = when (centered) {
        true -> -1f..1f
        false -> 0f..1f
    }

    RangeSlider(
        modifier = modifier,
        enabled = enabled,
        value = (mappedValue.start.forwardMap(exponent))..(mappedValue.endInclusive.forwardMap(exponent)),
        valueRange = mappedRange,
        onValueChange = { value ->
            val fraction = value.start.backwardMap(exponent)..value.endInclusive.backwardMap(exponent)
            val value = when (centered) {
                true -> (fraction.start * valueRange.endInclusive)..(fraction.endInclusive * valueRange.endInclusive)
                false -> (valueRange.start + fraction.start * travelAmount)..(valueRange.start + fraction.endInclusive * travelAmount)
            }
            onValueChange(value)
        },
        onValueChangeFinished = onValueChangeFinished,
        colors = colors
    )
}

private fun Float.forwardMap(exponent: Float) = if (this >= 0f) pow(exponent) else -(-this).pow(exponent)
private fun Float.backwardMap(exponent: Float) = if (this >= 0f) pow(1f / exponent) else -(-this).pow(1f / exponent)

@Preview
@Composable
private fun SketchpadSliderPreview() {
    var value by remember { mutableFloatStateOf(0.5f) }

    SketchpadTheme {
        Surface {
            Column {
                Text("$value")

                SketchpadSlider(
                    exponent = 0.5f,
                    value = value,
                    valueRange = 0f..1f,
                    onValueChange = { value = it }
                )
            }
        }
    }
}

@Preview
@Composable
private fun SketchpadCenteredSliderPreview() {
    var value by remember { mutableFloatStateOf(0.5f) }

    SketchpadTheme {
        Surface {
            Column {
                Text("$value")

                SketchpadCenteredSlider(
                    exponent = 0.5f,
                    value = value,
                    valueRange = 1f,
                    onValueChange = { value = it }
                )
            }
        }
    }
}

@Preview
@Composable
private fun SketchpadRangeSliderPreview(modifier: Modifier = Modifier) {
    var min by remember { mutableFloatStateOf(0f) }
    var max by remember { mutableFloatStateOf(0.5f) }

    SketchpadTheme {
        Surface {
            Column {
                Text("$min -> $max")

                SketchpadRangeSlider(
                    exponent = 0.5f,
                    value = min..max,
                    valueRange = -0.5f..1f,
                    onValueChange = {
                        min = it.start
                        max = it.endInclusive
                    }
                )

                SketchpadRangeSlider(
                    exponent = 0.5f,
                    value = min..max,
                    valueRange = -1f..1f,
                    onValueChange = {
                        min = it.start
                        max = it.endInclusive
                    }
                )
            }
        }
    }
}