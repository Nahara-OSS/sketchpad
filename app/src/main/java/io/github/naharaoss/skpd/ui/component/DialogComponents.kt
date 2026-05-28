package io.github.naharaoss.skpd.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun FancyDialog(
    onDismissRequest: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (icon != null) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.secondary) {
                            icon()
                        }
                    }

                    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.headlineSmall) {
                        title()
                    }
                }

                content()

                if (buttons != null) {
                    Row(Modifier.align(Alignment.End)) {
                        buttons()
                    }
                }
            }
        }
    }
}

@Composable
fun FancyDialogText(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
        LocalTextStyle provides MaterialTheme.typography.bodyMedium
    ) {
        content()
    }
}