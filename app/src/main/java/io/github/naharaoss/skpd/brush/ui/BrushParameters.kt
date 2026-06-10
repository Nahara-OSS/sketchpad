package io.github.naharaoss.skpd.brush.ui

import android.icu.text.DecimalFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.ui.component.FancyDialog
import io.github.naharaoss.skpd.ui.component.SketchpadCenteredSlider
import io.github.naharaoss.skpd.ui.component.SketchpadSlider
import java.text.ParseException

@Composable
fun BrushSliderParameter(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    formatValue: @Composable (Float) -> Unit,
    action: @Composable () -> Unit = {},
    exponent: Float = 1f,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
) {
    var manualInputDialog by rememberSaveable { mutableStateOf(false) }

    BrushParameterLayout(
        modifier = modifier,
        icon = icon,
        label = label,
        value = { formatValue(value) },
        action = action,
        onLabelClick = { manualInputDialog = true }
    ) {
        if (-valueRange.start == valueRange.endInclusive) {
            SketchpadCenteredSlider(
                enabled = enabled,
                exponent = exponent,
                value = value,
                valueRange = valueRange.endInclusive,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished
            )
        } else {
            SketchpadSlider(
                enabled = enabled,
                exponent = exponent,
                value = value,
                valueRange = valueRange,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished
            )
        }
    }

    if (manualInputDialog) {
        val formatter = remember { DecimalFormat("#,##0.####") }
        var value by remember(value) { mutableStateOf(formatter.format(value)) }
        val convertedValue = try {
            val value = formatter.parse(value)!!.toFloat()
            when {
                value < valueRange.start -> valueRange.start
                value > valueRange.endInclusive -> valueRange.endInclusive
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
                        onValueChange(convertedValue)
                        onValueChangeFinished()
                        manualInputDialog = false
                    },
                    content = { Text("Confirm") }
                )
            }
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = { value = it },
                label = { label() },
                isError = convertedValue == null,
                supportingText = {
                    when (convertedValue) {
                        null -> Text("Invalid format")
                        else -> formatValue(convertedValue)
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
    enabled: Boolean = true,
    onLabelClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    value: @Composable () -> Unit,
    action: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val rowModifier = if (onLabelClick != null) Modifier.clickable(enabled = enabled, onClick = onLabelClick) else Modifier

    Column(modifier) {
        Row(
            modifier = rowModifier.padding(16.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f).height(IntrinsicSize.Max),
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

        Box(Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp)) {
            content()
        }
    }
}