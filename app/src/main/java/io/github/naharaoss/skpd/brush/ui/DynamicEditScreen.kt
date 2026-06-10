package io.github.naharaoss.skpd.brush.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushPresetViewModel
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.Dynamic
import io.github.naharaoss.skpd.brush.Sensor
import io.github.naharaoss.skpd.ui.component.TooltipIconButton
import io.github.naharaoss.skpd.utils.Graph
import io.github.naharaoss.skpd.utils.GraphEditor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DynamicEditScreen(
    modifier: Modifier = Modifier,
    presetViewModel: BrushPresetViewModel,
    parameter: String,
    onBack: () -> Unit
) {
    val preset by presetViewModel.preset.collectAsState()

    AnimatedContent(
        modifier = modifier,
        targetState = preset,
        contentKey = { it != null }
    ) { preset ->
        when {
            preset == null -> {
                var showIndicator by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(200.milliseconds)
                    showIndicator = true
                }

                AnimatedVisibility(
                    modifier = Modifier.fillMaxSize(),
                    visible = showIndicator,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
            }

            else -> {
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                val parameter = preset.type.allParameters.find { it.parameter == parameter }!!
                val parameterName = stringResource(parameter.nameRes)
                val dynamic = parameter.getDynamicTypeErased(preset)
                val scope = rememberCoroutineScope()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        LargeFlexibleTopAppBar(
                            scrollBehavior = scrollBehavior,
                            title = { Text("Dynamic editor") },
                            subtitle = { Text(parameterName) },
                            navigationIcon = {
                                TooltipIconButton(
                                    painter = painterResource(R.drawable.arrow_back_24px),
                                    description = "Go back",
                                    onClick = onBack
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = innerPadding
                    ) {
                        item(key = "(base value)") {
                            var base by remember(dynamic.base) { mutableFloatStateOf(dynamic.base) }

                            BrushSliderParameter(
                                icon = { Icon(painterResource(parameter.iconRes), "Base value") },
                                label = { Text("Base value") },
                                formatValue = { Text(parameter.formatValue(it)) },
                                exponent = parameter.exponent,
                                value = base,
                                valueRange = parameter.valueRange,
                                onValueChange = { base = it },
                                onValueChangeFinished = {
                                    scope.launch {
                                        presetViewModel.updatePreset {
                                            val dynamic = parameter.getDynamicTypeErased(it)
                                            val preset = parameter.replaceDynamicTypeErased(it, dynamic.copy(base = base))
                                            preset
                                        }
                                    }
                                }
                            )
                        }

                        items(count = dynamic.modifiers.size, key = { dynamic.modifiers[it].id }) { i ->
                            val modifier = dynamic.modifiers[i]
                            val sensorName = stringResource(modifier.sensor.nameRes)
                            var showDetails by remember { mutableStateOf(false) }
                            var sensorDropdown by remember { mutableStateOf(false) }
                            var operationDropdown by remember { mutableStateOf(false) }

                            fun BrushType.Preset.updateModifier(updater: (Dynamic.Modifier) -> Dynamic.Modifier): BrushType.Preset {
                                val dynamic = parameter.getDynamicTypeErased(this)
                                val modifiers = dynamic.modifiers.map { if (it.id == modifier.id) updater(modifier) else it }
                                return parameter.replaceDynamicTypeErased(this, dynamic.copy(modifiers = modifiers))
                            }

                            ListItem(
                                selected = showDetails,
                                content = { Text(sensorName) },
                                supportingContent = {
                                    when (modifier.operation) {
                                        is Dynamic.Operation.Multiplicative -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Multiplicative")
                                            Text("\u2022")
                                            Text("${(modifier.operation.minGain * 100).roundToInt()}% to ${(modifier.operation.maxGain * 100).roundToInt()}%")
                                        }

                                        is Dynamic.Operation.Additive -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Additive")
                                            Text("\u2022")
                                            Text("${parameter.formatValue(modifier.operation.minValue)} to ${parameter.formatValue(modifier.operation.maxValue)}")
                                        }
                                    }
                                },
                                leadingContent = { Icon(painterResource(modifier.sensor.iconRes), sensorName) },
                                onClick = { showDetails = !showDetails }
                            )

                            AnimatedVisibility(modifier = Modifier.fillMaxWidth(), visible = showDetails) {
                                Column {
                                    ListItem(
                                        onClick = { sensorDropdown = true },
                                        content = { Text("Sensor type") },
                                        supportingContent = { Text(sensorName) },
                                        leadingContent = { Icon(painterResource(R.drawable.edit_24px), "Sensor type") },
                                        trailingContent = {
                                            DropdownMenu(
                                                expanded = sensorDropdown,
                                                onDismissRequest = { sensorDropdown = false }
                                            ) {
                                                Sensor.AllDefaults.forEach { sensor ->
                                                    val sensorName = stringResource(sensor.nameRes)
                                                    val selected = sensor.javaClass == modifier.sensor.javaClass

                                                    DropdownMenuItem(
                                                        leadingIcon = { Icon(painterResource(sensor.iconRes), sensorName) },
                                                        trailingIcon = { if (selected) Icon(painterResource(R.drawable.check_24px), "Selected") },
                                                        text = { Text(sensorName) },
                                                        onClick = {
                                                            scope.launch {
                                                                presetViewModel.updatePreset {
                                                                    it.updateModifier { mod -> mod.copy(sensor = sensor) }
                                                                }
                                                            }

                                                            sensorDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    AnimatedContent(
                                        targetState = modifier.sensor,
                                        contentKey = { it.javaClass }
                                    ) { sensor ->
                                        when (sensor) {
                                            is Sensor.Velocity -> {
                                                var max by remember(sensor.max) { mutableFloatStateOf(sensor.max) }

                                                BrushSliderParameter(
                                                    modifier = Modifier.padding(top = 8.dp),
                                                    icon = { Icon(painterResource(R.drawable.speed_24px), "Maximum speed") },
                                                    label = { Text("Maximum speed") },
                                                    formatValue = { Text("${if (it < 10) "%.2f".format(it) else it.roundToInt()} pixels per second") },
                                                    value = max,
                                                    valueRange = 1f..10000f,
                                                    onValueChange = { max = it },
                                                    onValueChangeFinished = {
                                                        scope.launch {
                                                            presetViewModel.updatePreset {
                                                                it.updateModifier { mod -> mod.copy(sensor = Sensor.Velocity(max)) }
                                                            }
                                                        }
                                                    }
                                                )
                                            }

                                            is Sensor.Time -> {
                                                var max by remember(sensor.max) { mutableFloatStateOf(sensor.max) }

                                                BrushSliderParameter(
                                                    modifier = Modifier.padding(top = 8.dp),
                                                    icon = { Icon(painterResource(R.drawable.timer_24px), "Maximum duration") },
                                                    label = { Text("Maximum duration") },
                                                    formatValue = { Text("${if (it < 10) "%.2f".format(it) else it.roundToInt()} seconds") },
                                                    value = max,
                                                    valueRange = 1f..60f,
                                                    onValueChange = { max = it },
                                                    onValueChangeFinished = {
                                                        scope.launch {
                                                            presetViewModel.updatePreset {
                                                                it.updateModifier { mod -> mod.copy(sensor = Sensor.Time(max)) }
                                                            }
                                                        }
                                                    }
                                                )
                                            }

                                            else -> Box(Modifier.fillMaxWidth())
                                        }
                                    }

                                    ListItem(
                                        onClick = { operationDropdown = true },
                                        content = { Text("Modifier operation") },
                                        supportingContent = {
                                            when (modifier.operation) {
                                                is Dynamic.Operation.Additive -> Text("Additive")
                                                is Dynamic.Operation.Multiplicative -> Text("Multiplicative")
                                            }
                                        },
                                        leadingContent = { Icon(painterResource(R.drawable.question_mark_24px), "Modifier operation") },
                                        trailingContent = {
                                            DropdownMenu(
                                                expanded = operationDropdown,
                                                onDismissRequest = { operationDropdown = false }
                                            ) {
                                                listOf(
                                                    Dynamic.Operation.Additive(0f, parameter.valueRange.endInclusive),
                                                    Dynamic.Operation.Multiplicative(0f, 1f)
                                                ).forEach { operation ->
                                                    DropdownMenuItem(
                                                        leadingIcon = { Icon(painterResource(R.drawable.question_mark_24px), null) },
                                                        trailingIcon = if (operation.javaClass == modifier.operation.javaClass) {
                                                            { Icon(painterResource(R.drawable.check_24px), "Selected") }
                                                        } else {
                                                            null
                                                        },
                                                        text = {
                                                            when (operation) {
                                                                is Dynamic.Operation.Additive -> Text("Additive")
                                                                is Dynamic.Operation.Multiplicative -> Text("Multiplicative")
                                                            }
                                                        },
                                                        onClick = {
                                                            scope.launch {
                                                                presetViewModel.updatePreset {
                                                                    it.updateModifier { mod -> mod.copy(operation = operation) }
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    AnimatedContent(
                                        targetState = modifier.operation,
                                        contentKey = { it.javaClass },
                                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                                    ) { operation ->
                                        when (operation) {
                                            is Dynamic.Operation.Additive -> {
                                                var minValue by remember { mutableFloatStateOf(operation.minValue) }
                                                var maxValue by remember { mutableFloatStateOf(operation.maxValue) }
                                                val additiveRange = max(abs(parameter.valueRange.start), abs(parameter.valueRange.endInclusive))

                                                BrushParameterLayout(
                                                    icon = { Icon(painterResource(R.drawable.question_mark_24px), "Value") },
                                                    label = { Text("Modifier addition") },
                                                    value = { Text("${parameter.formatValue(minValue)} to ${parameter.formatValue(maxValue)}") },
                                                    action = {}
                                                ) {
                                                    RangeSlider(
                                                        value = minValue..maxValue,
                                                        valueRange = -additiveRange..additiveRange,
                                                        onValueChange = {
                                                            minValue = it.start
                                                            maxValue = it.endInclusive
                                                        },
                                                        onValueChangeFinished = {
                                                            scope.launch {
                                                                presetViewModel.updatePreset {
                                                                    it.updateModifier { mod -> mod.copy(operation = Dynamic.Operation.Additive(minValue, maxValue)) }
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }

                                            is Dynamic.Operation.Multiplicative -> {
                                                var minGain by remember { mutableFloatStateOf(operation.minGain) }
                                                var maxGain by remember { mutableFloatStateOf(operation.maxGain) }

                                                BrushParameterLayout(
                                                    icon = { Icon(painterResource(R.drawable.question_mark_24px), "Value") },
                                                    label = { Text("Modifier gain") },
                                                    value = { Text("${(minGain * 100f).roundToInt()}% to ${(maxGain * 100f).roundToInt()}%") },
                                                    action = {}
                                                ) {
                                                    RangeSlider(
                                                        value = minGain..maxGain,
                                                        valueRange = -1f..1f,
                                                        onValueChange = {
                                                            minGain = it.start
                                                            maxGain = it.endInclusive
                                                        },
                                                        onValueChangeFinished = {
                                                            scope.launch {
                                                                presetViewModel.updatePreset {
                                                                    it.updateModifier { mod -> mod.copy(operation = Dynamic.Operation.Multiplicative(minGain, maxGain)) }
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    ListItem(
                                        onClick = {
                                            scope.launch {
                                                presetViewModel.updatePreset {
                                                    val dynamic = parameter.getDynamicTypeErased(it)
                                                    val modifiers = dynamic.modifiers.filter { mod -> mod.id != modifier.id }
                                                    parameter.replaceDynamicTypeErased(it, dynamic.copy(modifiers = modifiers) )
                                                }
                                            }
                                        },
                                        colors = ListItemDefaults.colors(contentColor = MaterialTheme.colorScheme.error),
                                        content = { Text("Delete modifier") },
                                        supportingContent = { Text("Remove this modifier from the dynamic") },
                                        leadingContent = { Icon(painterResource(R.drawable.delete_24px), "Delete modifier") }
                                    )

                                    GraphEditor(
                                        modifier = Modifier.fillMaxWidth().height(300.dp),
                                        enabled = true,
                                        graph = modifier.graph,
                                        onGraphChange = { graph ->
                                            scope.launch {
                                                presetViewModel.updatePreset {
                                                    it.updateModifier { mod -> mod.copy(graph = graph) }
                                                }
                                            }
                                        },
                                        xAxisLabel = { Text(sensorName) },
                                        yAxisLabel = { Text(parameterName) }
                                    )
                                }
                            }
                        }

                        item(key = "(add new modifier)") {
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(dynamic.modifiers.size, dynamic.modifiers.size + 1),
                                content = { Text("Add new modifier") },
                                supportingContent = { Text("Change value based on stylus sensor") },
                                leadingContent = { Icon(painterResource(R.drawable.add_24px), "Add") },
                                onClick = {
                                    val modifier = Dynamic.Modifier(
                                        id = UUID.randomUUID().toString(),
                                        sensor = Sensor.Pressure,
                                        operation = Dynamic.Operation.Multiplicative(0f, 1f),
                                        graph = Graph()
                                    )

                                    scope.launch {
                                        presetViewModel.updatePreset {
                                            val dynamic = parameter.getDynamicTypeErased(it)
                                            parameter.replaceDynamicTypeErased(it, dynamic.copy(modifiers = dynamic.modifiers + modifier))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}