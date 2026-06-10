package io.github.naharaoss.skpd.settings.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import io.github.naharaoss.skpd.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutSettingsPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fullName = stringResource(R.string.app_name_full)
    val license = stringResource(R.string.app_license)
    val version = stringResource(R.string.app_version)
    val git = stringResource(R.string.app_git_hash)
    val sourceUrl = stringResource(R.string.app_source_url).toUri()

    Column(modifier.verticalScroll(rememberScrollState())) {
        ListItem(
            onClick = {},
            content = { Text(stringResource(R.string.settings_about_version_title)) },
            supportingContent = { Text(stringResource(R.string.settings_about_version_subtitle).format(version, git)) }
        )
        ListItem(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, sourceUrl)
                context.startActivity(intent)
            },
            content = { Text(stringResource(R.string.settings_about_source_title)) },
            supportingContent = { Text(stringResource(R.string.settings_about_source_subtitle).format(fullName, license)) }
        )
    }
}