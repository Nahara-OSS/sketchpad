package io.github.naharaoss.skpd.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.naharaoss.skpd.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutSettingsPage(
    modifier: Modifier = Modifier,
    onOpenSourceCode: () -> Unit
) {
    val fullName = stringResource(R.string.app_name_full)
    val license = stringResource(R.string.app_license)
    val version = stringResource(R.string.app_version)
    val git = stringResource(R.string.app_git_hash)

    Column(modifier.verticalScroll(rememberScrollState())) {
        ListItem(
            onClick = {},
            content = { Text(stringResource(R.string.settings_about_version_title)) },
            supportingContent = { Text(stringResource(R.string.settings_about_version_subtitle).format(version, git)) }
        )
        ListItem(
            onClick = onOpenSourceCode,
            content = { Text(stringResource(R.string.settings_about_source_title)) },
            supportingContent = { Text(stringResource(R.string.settings_about_source_subtitle).format(fullName, license)) }
        )
    }
}