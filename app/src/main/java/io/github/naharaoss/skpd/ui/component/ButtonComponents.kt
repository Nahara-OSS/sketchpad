package io.github.naharaoss.skpd.ui.component

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun TooltipIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    painter: Painter,
    description: String,
    onClick: () -> Unit
) {
    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = {
            PlainTooltip { Text(description) }
        }
    ) {
        when {
            filled -> FilledIconButton(
                modifier = modifier,
                enabled = enabled,
                onClick = onClick
            ) {
                Icon(
                    painter = painter,
                    contentDescription = description
                )
            }

            else -> IconButton(
                modifier = modifier,
                enabled = enabled,
                onClick = onClick
            ) {
                Icon(
                    painter = painter,
                    contentDescription = description
                )
            }
        }
    }
}