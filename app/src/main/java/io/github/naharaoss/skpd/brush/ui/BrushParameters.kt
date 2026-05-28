package io.github.naharaoss.skpd.brush.ui

import android.icu.text.DecimalFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.Dynamic
import io.github.naharaoss.skpd.ui.component.FancyDialog
import java.text.ParseException

@Composable
fun DynamicSlider(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    value: @Composable (Float) -> Unit,
    dynamic: Dynamic,
    onDynamicChange: (Dynamic) -> Unit,
    onDynamicChangeFinished: () -> Unit,
    onSensorEdit: () -> Unit,
    range: ClosedFloatingPointRange<Float>,
    sliderMapping: Pair<(Float) -> Float, (Float) -> Float> = Pair({ it }, { it }),
    sliderColors: SliderColors = SliderDefaults.colors(),
    sliderTrack: @Composable (SliderState) -> Unit = { sliderState -> SliderDefaults.Track(colors = sliderColors, enabled = enabled, sliderState = sliderState) }
) {
    var manualInputDialog by rememberSaveable { mutableStateOf(false) }

    BrushParameterLayout(
        modifier = modifier,
        icon = icon,
        label = label,
        value = { value(dynamic.base) },
        action = {
            ToggleButton(
                checked = dynamic.modifiers.isNotEmpty(),
                onCheckedChange = { onSensorEdit() }
            ) {
                Icon(painterResource(R.drawable.edit_24px), "Sensor")
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Sensor")
            }
        },
        content = {
            Slider(
                value = sliderMapping.first(dynamic.base),
                valueRange = sliderMapping.first(range.start)..sliderMapping.first(range.endInclusive),
                onValueChange = { onDynamicChange(dynamic.copy(base = sliderMapping.second(it))) },
                onValueChangeFinished = onDynamicChangeFinished,
                track = sliderTrack
            )
        },
        onLabelClick = { manualInputDialog = true }
    )

    if (manualInputDialog) {
        val formatter = remember { DecimalFormat("#,##0.####") }
        var value by remember(dynamic) { mutableStateOf(formatter.format(dynamic.base)) }
        val convertedValue = try {
            val value = formatter.parse(value)!!.toFloat()
            when {
                value < range.start -> range.start
                value > range.endInclusive -> range.endInclusive
                else -> value
            }
        } catch (e: ParseException) {
            null
        }

        FancyDialog(
            onDismissRequest = { manualInputDialog = false },
            icon = { icon() },
            title = { label() },
            buttons = {
                TextButton(
                    onClick = { manualInputDialog = false },
                    content = { Text("Cancel") }
                )
                TextButton(
                    enabled = convertedValue != null,
                    onClick = {
                        if (convertedValue == null) return@TextButton
                        onDynamicChange(dynamic.copy(base = convertedValue))
                        onDynamicChangeFinished()
                        manualInputDialog = false
                    },
                    content = { Text("Confirm") }
                )
            }
        ) {
            TextField(
                value = value,
                onValueChange = { value = it },
                label = { label() },
                isError = convertedValue == null,
                supportingText = {
                    when (convertedValue) {
                        null -> Text("Invalid format")
                        else -> value(convertedValue)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberSigned)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrushParameterLayout(
    modifier: Modifier = Modifier,
    onLabelClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    value: @Composable () -> Unit,
    action: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp, 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rowModifier = if (onLabelClick != null) Modifier.clickable(enabled = true, onClick = onLabelClick) else Modifier

            Row(
                modifier = rowModifier.weight(1f).height(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    icon()
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
                        label()
                    }

                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        value()
                    }
                }
            }

            action()
        }

        content()
    }
}