package io.github.naharaoss.skpd.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.settings.InputSettings

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InputSettingsPage(
    modifier: Modifier = Modifier,
    settings: InputSettings,
    onSettingsChange: (InputSettings) -> Unit,
    onOpenInputTester: () -> Unit
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        ListItem(
            onClick = {},
            leadingContent = { Icon(painterResource(R.drawable.touch_app_24px), null) },
            content = { Text("Touch drawing") },
            supportingContent = { Text("Enable drawing with finger") },
            trailingContent = {
                Switch(
                    checked = settings.fingerDrawing,
                    onCheckedChange = { onSettingsChange(settings.copy(fingerDrawing = it)) }
                )
            }
        )

        ListItem(
            onClick = onOpenInputTester,
            leadingContent = { Icon(painterResource(R.drawable.edit_24px), null) },
            content = { Text("Open input tester") },
            supportingContent = { Text("Test pen and touch inputs") }
        )
    }
}