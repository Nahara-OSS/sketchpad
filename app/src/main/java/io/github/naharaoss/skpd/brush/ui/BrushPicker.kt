package io.github.naharaoss.skpd.brush.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.minus
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushListViewModel
import io.github.naharaoss.skpd.brush.BrushPresetViewModel
import io.github.naharaoss.skpd.resource.BrushItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrushPicker(
    modifier: Modifier = Modifier,
    listViewModel: BrushListViewModel,
    selected: BrushItem?,
    padding: PaddingValues,
    compact: Boolean = false,
    onAddTag: (() -> Unit)? = null,
    onAddBrush: (() -> Unit)? = null,
    onBrushSelect: (BrushItem) -> Unit
) {
    val dir = LocalLayoutDirection.current
    val brushes by listViewModel.brushes.collectAsState()
    val keyword by listViewModel.keyword.collectAsState()

    @Composable
    fun CategoryButton(
        modifier: Modifier = Modifier,
        enabled: Boolean,
        selected: Boolean,
        onSelect: () -> Unit,
        icon: @Composable () -> Unit,
        label: @Composable () -> Unit
    ) {
        when (compact) {
            true -> TooltipBox(
                state = rememberTooltipState(),
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.End),
                tooltip = {
                    PlainTooltip { label() }
                }
            ) {
                FilledIconToggleButton(
                    modifier = modifier,
                    enabled = enabled,
                    checked = selected,
                    onCheckedChange = { onSelect() }
                ) {
                    icon()
                }
            }

            false -> ToggleButton(
                modifier = modifier.fillMaxWidth(),
                enabled = enabled,
                checked = selected,
                onCheckedChange = { onSelect() }
            ) {
                icon()
                Spacer(Modifier.width(4.dp))
                label()
                Spacer(Modifier.weight(1f))
            }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 180.dp)
                .padding(padding - PaddingValues(end = padding.calculateEndPadding(dir)))
        ) {
            item {
                CategoryButton(
                    enabled = false,
                    selected = true,
                    onSelect = {},
                    icon = { Icon(painterResource(R.drawable.edit_24px), stringResource(R.string.tag_all)) },
                    label = { Text(stringResource(R.string.tag_all)) }
                )
            }

            item {
                CategoryButton(
                    enabled = false,
                    selected = false,
                    onSelect = {},
                    icon = { Icon(painterResource(R.drawable.question_mark_24px), stringResource(R.string.tag_uncategorized)) },
                    label = { Text(stringResource(R.string.tag_uncategorized)) }
                )
            }

            if (onAddTag != null) {
                item {
                    when (compact) {
                        true -> IconButton(onAddTag) {
                            Icon(
                                painter = painterResource(R.drawable.add_24px),
                                contentDescription = stringResource(R.string.tag_add)
                            )
                        }

                        false -> TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onAddTag
                        ) {
                            Icon(painterResource(R.drawable.add_24px), stringResource(R.string.tag_add))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.tag_add))
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = brushes,
            contentKey = { it != null },
            transitionSpec = {
                val enter = fadeIn()
                val exit = fadeOut()
                enter togetherWith exit
            }
        ) { brushes ->
            when (brushes) {
                null -> Box(Modifier.fillMaxSize()) {
                    LoadingIndicator(Modifier.align(Alignment.Center))
                }

                else -> {
                    val vmOwner = rememberViewModelStoreOwner()

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = padding - PaddingValues(start = padding.calculateStartPadding(dir)),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(brushes, key = { it.id }) { brush ->
                            val presetViewModel = hiltViewModel(
                                viewModelStoreOwner = vmOwner,
                                key = "BrushItem(${brush.id})",
                                creationCallback = { factory: BrushPresetViewModel.Factory -> factory.create(brush) }
                            )

                            val brush by presetViewModel.brush.collectAsState()
                            val preset by presetViewModel.preset.collectAsState()

                            BrushCard(
                                modifier = Modifier.height(96.dp),
                                selected = selected?.id == brush.id,
                                onBrushClick = { onBrushSelect(brush) },
                                onIconClick = {},
                                label = { Text(brush.name) },
                                iconId = brush.icon,
                                preset = preset,
                                favorite = false,
                                onFavoriteChange = {},
                            )
                        }

                        if (onAddBrush != null) {
                            item {
                                BrushCardLayout(
                                    modifier = Modifier.height(96.dp),
                                    selected = false,
                                    onClick = onAddBrush,
                                    label = { Text(stringResource(R.string.brush_add)) },
                                    icons = {}
                                ) {
                                    Icon(
                                        modifier = Modifier.align(Alignment.Center),
                                        painter = painterResource(R.drawable.add_24px),
                                        contentDescription = stringResource(R.string.brush_add)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}