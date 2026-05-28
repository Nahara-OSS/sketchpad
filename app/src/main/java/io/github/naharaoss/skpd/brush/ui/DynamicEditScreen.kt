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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.brush.Dynamic
import io.github.naharaoss.skpd.brush.Sensor
import io.github.naharaoss.skpd.ui.component.TooltipIconButton
import io.github.naharaoss.skpd.utils.Graph
import io.github.naharaoss.skpd.utils.GraphEditor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DynamicEditScreen(
    modifier: Modifier = Modifier,
    viewModel: BrushListViewModel,
    brushId: Long,
    parameter: String,
    onBack: () -> Unit
) {
    val brushes by viewModel.brushes.collectAsState()
    val brush = brushes?.find { it.id == brushId }
    val presetState = brush?.preset?.collectAsState(brush.preset.replayCache.lastOrNull())

    AnimatedContent(
        modifier = modifier,
        targetState = presetState,
        contentKey = { it != null }
    ) { presetState ->
        when {
            presetState == null || presetState.value == null -> {
                var showIndicator by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(200)
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
                val preset by presetState
                val parameter = preset!!.type.allParameters.find { it.parameter == parameter }!!
                val parameterName = stringResource(parameter.nameRes)
                var decoupledPreset by remember(preset) { mutableStateOf(preset!!) }
                val dynamic = parameter.getDynamicTypeErased(decoupledPreset)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = innerPadding
                    ) {
                        item {
                            BrushParameterLayout(
                                modifier = Modifier.padding(top = 8.dp),
                                icon = { Icon(painterResource(parameter.iconRes), "Base value") },
                                label = { Text("Base value") },
                                value = { Text(parameter.formatValue(dynamic.base)) },
                                action = {}
                            ) {
                                Slider(
                                    value = parameter.forwardMapToSlider(dynamic.base),
                                    valueRange = parameter.forwardMapToSlider(parameter.min)..parameter.forwardMapToSlider(parameter.max),
                                    onValueChange = {
                                        val dynamic = dynamic.copy(base = parameter.backwardMapToSlider(it))
                                        decoupledPreset = parameter.replaceDynamicTypeErased(decoupledPreset, dynamic)
                                    },
                                    onValueChangeFinished = {
                                        scope.launch { brush?.store(decoupledPreset) }
                                    },
                                    track = when (parameter.centered) {
                                        true -> { sliderState -> SliderDefaults.CenteredTrack(colors = SliderDefaults.colors(), sliderState = sliderState) }
                                        false -> { sliderState -> SliderDefaults.Track(colors = SliderDefaults.colors(), sliderState = sliderState) }
                                    }
                                )
                            }
                        }

                        items(count = dynamic.modifiers.size, key = { dynamic.modifiers[it].id }) { i ->
                            val modifier = dynamic.modifiers[i]
                            val sensorName = stringResource(modifier.sensor.nameRes)
                            var showGraph by remember { mutableStateOf(false) }
                            var decoupledGraph by remember { mutableStateOf(modifier.graph) }
                            var sensorDropdown by remember { mutableStateOf(false) }
                            var operationDropdown by remember { mutableStateOf(false) }

                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(i, dynamic.modifiers.size + 1),
                                selected = showGraph,
                                content = { Text(sensorName) },
                                supportingContent = {
                                    when (modifier.operation) {
                                        is Dynamic.Operation.Multiplicative -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Multiplicative")
                                            Text("\u2022")
                                            Text("${(modifier.operation.gain * 100).roundToInt()}%")
                                        }

                                        is Dynamic.Operation.Additive -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Additive")
                                            Text("\u2022")
                                            Text(parameter.formatValue(modifier.operation.value))
                                        }
                                    }
                                },
                                leadingContent = { Icon(painterResource(modifier.sensor.iconRes), sensorName) },
                                onClick = { showGraph = !showGraph }
                            )

                            AnimatedVisibility(
                                modifier = Modifier.fillMaxWidth(),
                                visible = showGraph
                            ) {
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

                                                    DropdownMenuItem(
                                                        leadingIcon = { Icon(painterResource(sensor.iconRes), sensorName) },
                                                        trailingIcon = if (sensor.javaClass == modifier.sensor.javaClass) {
                                                            { Icon(painterResource(R.drawable.check_24px), "Selected") }
                                                        } else {
                                                            null
                                                        },
                                                        text = { Text(sensorName) },
                                                        onClick = {
                                                            val modifier = modifier.copy(sensor = sensor)
                                                            val dynamic = dynamic.copy(modifiers = dynamic.modifiers.map { if (it.id == modifier.id) modifier else it })
                                                            val preset = parameter.replaceDynamicTypeErased(decoupledPreset, dynamic)
                                                            decoupledPreset = preset
                                                            sensorDropdown = false
                                                            scope.launch { brush?.store(preset) }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    )

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
                                                    Dynamic.Operation.Additive(parameter.max),
                                                    Dynamic.Operation.Multiplicative(1f)
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
                                                            val modifier = modifier.copy(operation = operation)
                                                            val dynamic = dynamic.copy(modifiers = dynamic.modifiers.map { if (it.id == modifier.id) modifier else it })
                                                            val preset = parameter.replaceDynamicTypeErased(decoupledPreset, dynamic)
                                                            decoupledPreset = preset
                                                            operationDropdown = false
                                                            scope.launch { brush?.store(preset) }
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
                                                var value by remember { mutableFloatStateOf(operation.value) }
                                                val mappedMax = parameter.forwardMapToSlider(parameter.max)

                                                BrushParameterLayout(
                                                    modifier = Modifier.padding(top = 8.dp),
                                                    icon = { Icon(painterResource(R.drawable.question_mark_24px), "Value") },
                                                    label = { Text("Modifier addition") },
                                                    value = { Text((if (value >= 0) "+" else "") + parameter.formatValue(value)) },
                                                    action = {}
                                                ) {
                                                    Slider(
                                                        value = if (value >= 0f) parameter.forwardMapToSlider(value) else -parameter.forwardMapToSlider(-value),
                                                        valueRange = -mappedMax..mappedMax,
                                                        onValueChange = {
                                                            val input = if (it >= 0f) parameter.backwardMapToSlider(it) else -parameter.backwardMapToSlider(-it)
                                                            value = input
                                                        },
                                                        onValueChangeFinished = {
                                                            val modifier = modifier.copy(operation = operation.copy(value = value))
                                                            val dynamic = dynamic.copy(modifiers = dynamic.modifiers.map { if (it.id == modifier.id) modifier else it })
                                                            val preset = parameter.replaceDynamicTypeErased(decoupledPreset, dynamic)
                                                            decoupledPreset = preset
                                                            scope.launch { brush?.store(preset) }
                                                        },
                                                        track = { SliderDefaults.CenteredTrack(sliderState = it) }
                                                    )
                                                }
                                            }

                                            is Dynamic.Operation.Multiplicative -> {
                                                var gain by remember { mutableFloatStateOf(operation.gain) }

                                                BrushParameterLayout(
                                                    modifier = Modifier.padding(top = 8.dp),
                                                    icon = { Icon(painterResource(R.drawable.question_mark_24px), "Gain") },
                                                    label = { Text("Modifier gain") },
                                                    value = { Text((if (gain >= 0) "+" else "") + (gain * 100).roundToInt() + "%") },
                                                    action = {}
                                                ) {
                                                    Slider(
                                                        value = gain,
                                                        valueRange = -1f..1f,
                                                        onValueChange = { gain = it },
                                                        onValueChangeFinished = {
                                                            val modifier = modifier.copy(operation = operation.copy(gain = gain))
                                                            val dynamic = dynamic.copy(modifiers = dynamic.modifiers.map { if (it.id == modifier.id) modifier else it })
                                                            val preset = parameter.replaceDynamicTypeErased(decoupledPreset, dynamic)
                                                            decoupledPreset = preset
                                                            scope.launch { brush?.store(preset) }
                                                        },
                                                        track = { SliderDefaults.CenteredTrack(sliderState = it) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    ListItem(
                                        onClick = {
                                            val dynamic = dynamic.copy(modifiers = dynamic.modifiers.filter { it.id != modifier.id })
                                            val preset = parameter.replaceDynamicTypeErased(decoupledPreset, dynamic)
                                            decoupledPreset = preset
                                            scope.launch { brush?.store(preset) }
                                        },
                                        colors = ListItemDefaults.colors(contentColor = MaterialTheme.colorScheme.error),
                                        content = { Text("Delete modifier") },
                                        supportingContent = { Text("Remove this modifier from the dynamic") },
                                        leadingContent = { Icon(painterResource(R.drawable.delete_24px), "Delete modifier") }
                                    )

                                    GraphEditor(
                                        modifier = Modifier.fillMaxWidth().height(300.dp),
                                        enabled = true,
                                        graph = decoupledGraph,
                                        onGraphChange = {
                                            val modifier = modifier.copy(graph = it)
                                            val dynamic = dynamic.copy(modifiers = dynamic.modifiers.map { if (it.id == modifier.id) modifier else it })
                                            val preset = parameter.replaceDynamicTypeErased(decoupledPreset, dynamic)
                                            decoupledPreset = preset
                                            decoupledGraph = it
                                            scope.launch { brush?.store(preset) }
                                        },
                                        xAxisLabel = { Text(sensorName) },
                                        yAxisLabel = { Text(parameterName) }
                                    )
                                }
                            }
                        }

                        item {
                            SegmentedListItem(
                                shapes = ListItemDefaults.segmentedShapes(dynamic.modifiers.size, dynamic.modifiers.size + 1),
                                content = { Text("Add new modifier") },
                                supportingContent = { Text("Change value based on stylus sensor") },
                                leadingContent = { Icon(painterResource(R.drawable.add_24px), "Add") },
                                onClick = {
                                    val modifier = Dynamic.Modifier(
                                        id = UUID.randomUUID().toString(),
                                        sensor = Sensor.Pressure,
                                        operation = Dynamic.Operation.Multiplicative(gain = 1f),
                                        graph = Graph()
                                    )

                                    val dynamic = dynamic.copy(modifiers = dynamic.modifiers + modifier)
                                    val preset = parameter.replaceDynamicTypeErased(decoupledPreset, dynamic)
                                    decoupledPreset = preset
                                    scope.launch { brush?.store(preset) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}