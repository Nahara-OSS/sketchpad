package io.github.naharaoss.skpd.brush.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.minus
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.ui.component.TooltipIconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrushEditScreen(
    modifier: Modifier = Modifier,
    viewModel: BrushListViewModel,
    brushId: Long,
    onBack: () -> Unit,
    onDynamicEditor: (Long, String) -> Unit
) {
    val brushes by viewModel.brushes.collectAsState()
    val brush = brushes?.find { it.id == brushId }

    AnimatedContent(
        modifier = modifier,
        targetState = brush,
        contentKey = { it != null }
    ) { brush ->
        when (brush) {
            null -> {
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
                val name by brush.name.collectAsState()
                val icon by brush.icon.collectAsState()
                val preset by brush.preset.collectAsState(brush.preset.replayCache.lastOrNull())
                var deferredPreset by remember(preset) { mutableStateOf(preset) }
                val scope = rememberCoroutineScope()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        LargeFlexibleTopAppBar(
                            scrollBehavior = scrollBehavior,
                            title = { Text(name) },
                            subtitle = {
                                when (preset) {
                                    is StampBrush.Preset -> Text("Stamp-based brush")
                                    else -> Text("Loading preset")
                                }
                            },
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding - PaddingValues(bottom = innerPadding.calculateBottomPadding()))
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrushCard(
                                modifier = Modifier.fillMaxSize().height(96.dp),
                                preset = deferredPreset,
                                favorite = false,
                                selected = false,
                                label = { Text(name) },
                                iconId = icon,
                                onBrushClick = {},
                                onIconClick = {},
                                onFavoriteChange = {}
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                ToggleButton(
                                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                                    enabled = false,
                                    checked = false,
                                    onCheckedChange = {},
                                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                                ) {
                                    Icon(painterResource(R.drawable.edit_24px), "Simple")
                                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                                    Text("Simple")
                                }

                                ToggleButton(
                                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                                    checked = preset is StampBrush.Preset,
                                    onCheckedChange = {},
                                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                                ) {
                                    Icon(painterResource(R.drawable.edit_24px), "Stamp")
                                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                                    Text("Stamp")
                                }

                                ToggleButton(
                                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                                    enabled = false,
                                    checked = false,
                                    onCheckedChange = {},
                                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                                ) {
                                    Icon(painterResource(R.drawable.edit_24px), "Strip")
                                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                                    Text("Strip")
                                }
                            }
                        }

                        HorizontalDivider(Modifier.fillMaxWidth())

                        deferredPreset?.let { preset ->
                            when (preset) {
                                is StampBrush.Preset -> StampPresetEditor(
                                    modifier = Modifier.fillMaxWidth(),
                                    preset = preset,
                                    onPresetChange = { deferredPreset = it },
                                    onPresetChangeFinished = { scope.launch { brush.store(deferredPreset ?: preset) } },
                                    onDynamicEditor = { onDynamicEditor(brushId, it) }
                                )
                            }
                        }

                        Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
                    }
                }
            }
        }
    }
}