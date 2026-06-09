package io.github.naharaoss.skpd.ui.component

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun SketchpadPopup(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    titleBar: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val transitionState = remember { MutableTransitionState(initialState = false) }
    transitionState.targetState = visible

    val transition = rememberTransition(transitionState)
    val scale by transition.animateFloat(transitionSpec = { MaterialTheme.motionScheme.fastSpatialSpec() }) { if (it) 1f else 0.8f }
    val alpha by transition.animateFloat(transitionSpec = { MaterialTheme.motionScheme.fastEffectsSpec() }) { if (it) 1f else 0f }

    if (transitionState.currentState || transitionState.targetState) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            SketchpadPopupContent(
                modifier = modifier.graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                ),
                titleBar = titleBar,
                content = content
            )
        }
    }
}

@Composable
fun SketchpadPopupContent(
    modifier: Modifier = Modifier,
    titleBar: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            if (titleBar != null) {
                titleBar()
                HorizontalDivider(Modifier.fillMaxWidth())
            }

            content()
        }
    }
}

@Composable
fun SketchpadPopupTitleBar(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .padding(start = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            label()
        }

        Row {
            buttons()
        }
    }
}