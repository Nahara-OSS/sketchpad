package io.github.naharaoss.skpd.brush.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushPresetViewModel
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.resource.BrushItem
import io.github.naharaoss.skpd.ui.component.TooltipIconButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrushEditScreen(
    modifier: Modifier = Modifier,
    presetViewModel: BrushPresetViewModel,
    onBack: () -> Unit,
    onDynamicEditor: (BrushItem, String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val brush by presetViewModel.brush.collectAsState()
    val preset by presetViewModel.preset.collectAsState()
    var pendingChange: ((BrushType.Preset) -> BrushType.Preset)? by remember { mutableStateOf(null) }
    val pendingPreset = preset?.let { preset -> pendingChange?.let { it(preset) } } ?: preset

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeFlexibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(brush.name) },
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
                    preset = pendingPreset,
                    favorite = false,
                    selected = false,
                    label = { Text(brush.name) },
                    iconId = brush.icon,
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

            when (pendingPreset) {
                is StampBrush.Preset -> StampPresetEditor(
                    modifier = Modifier.fillMaxWidth(),
                    preset = pendingPreset,
                    onDynamicEditor = { onDynamicEditor(brush, it) },
                    onPresetChange = { updater ->
                        pendingChange = { if (it is StampBrush.Preset) updater(it) else it }
                    },
                    onPresetChangeFinished = {
                        pendingChange?.let { pendingChange ->
                            scope.launch {
                                presetViewModel.updatePreset(pendingChange)
                            }
                        }

                        pendingChange = null
                    }
                )
            }

            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}