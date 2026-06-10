package io.github.naharaoss.skpd.settings.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.naharaoss.skpd.R
import io.github.naharaoss.skpd.brush.BrushEditorActivity
import io.github.naharaoss.skpd.settings.MiscellaneousSettings

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiscellaneousSettingsPage(
    modifier: Modifier = Modifier,
    settings: MiscellaneousSettings,
    onSettingsChange: (MiscellaneousSettings) -> Unit
) {
    val context = LocalContext.current

    Column(modifier.verticalScroll(rememberScrollState())) {
        ListItem(
            onClick = {},
            content = { Text(stringResource(R.string.settings_miscellaneous_promotional_artworks_title)) },
            supportingContent = { Text(stringResource(R.string.settings_miscellaneous_promotional_artworks_subtitle)) },
            trailingContent = {
                Switch(
                    checked = settings.promotionalArtworks,
                    onCheckedChange = { onSettingsChange(settings.copy(promotionalArtworks = it)) }
                )
            }
        )

        ListItem(
            onClick = {
                val intent = Intent(context, BrushEditorActivity::class.java)
                context.startActivity(intent)
            },
            content = { Text("Open brush editor") },
            supportingContent = { Text("Manage and edit brushes") }
        )
    }
}