package io.github.naharaoss.skpd.brush.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushType
import io.github.naharaoss.skpd.ui.component.resourceIdFromNamedIcon
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrushCard(
    modifier: Modifier = Modifier,
    preset: BrushType.Preset?,
    enabled: Boolean = true,
    favorite: Boolean,
    selected: Boolean,
    label: @Composable () -> Unit,
    iconId: String?,
    onBrushClick: () -> Unit,
    onIconClick: () -> Unit,
    onFavoriteChange: (Boolean) -> Unit
) {
    BrushCardLayout(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        onClick = onBrushClick,
        label = label,
        icons = {
            IconButton({ onFavoriteChange(!favorite) }) {
                Icon(
                    painter = painterResource(if (favorite) R.drawable.star_filled_24px else R.drawable.star_24px),
                    contentDescription = stringResource(if (favorite) R.string.brush_unfavorite else R.string.brush_favorite)
                )
            }

            IconButton(onIconClick) {
                Icon(
                    painter = painterResource(resourceIdFromNamedIcon(iconId ?: "Pen")),
                    contentDescription = "Brush icon"
                )
            }
        },
        content = {
            AnimatedContent(
                targetState = preset,
                contentKey = { it != null },
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { preset ->
                when (preset) {
                    null -> Box(Modifier.fillMaxSize()) {
                        var loadingIndicator by remember { mutableStateOf(false) }

                        LaunchedEffect(preset) {
                            delay(200)
                            loadingIndicator = true
                        }

                        AnimatedVisibility(
                            modifier = Modifier.align(Alignment.Center),
                            visible = loadingIndicator,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            LoadingIndicator()
                        }
                    }

                    else -> BrushPreview(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        preset = preset
                    )
                }
            }
        }
    )
}

@Composable
fun BrushCardLayout(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    icons: @Composable RowScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        onClick = onClick,
        enabled = enabled,
        color = surfaceColor
    ) {
        Box(Modifier.fillMaxSize()) {
            content()

            Box(Modifier.align(Alignment.BottomStart).padding(16.dp, 8.dp)) {
                label()
            }

            Row(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                icons()
            }
        }
    }
}