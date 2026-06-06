package io.github.naharaoss.skpd.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.naharaoss.skpd.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainSettingsPage(
    modifier: Modifier = Modifier,
    currentRoute: SettingsRoute,
    onNavigate: (SettingsRoute) -> Unit
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
//        ListItem(
//            onClick = { onNavigate(SettingsRoute.Appearance) },
//            selected = currentRoute is SettingsRoute.Appearance,
//            leadingContent = { Icon(painterResource(R.drawable.style_24px), null) },
//            content = { Text(stringResource(R.string.settings_category_appearance_title)) },
//            supportingContent = { Text(stringResource(R.string.settings_category_appearance_subtitle)) }
//        )
//        ListItem(
//            onClick = { onNavigate(SettingsRoute.Toolbars) },
//            selected = currentRoute is SettingsRoute.Toolbars,
//            leadingContent = { Icon(painterResource(R.drawable.toolbar_24px), null) },
//            content = { Text(stringResource(R.string.settings_category_toolbars_title)) },
//            supportingContent = { Text(stringResource(R.string.settings_category_toolbars_subtitle)) }
//        )
        ListItem(
            onClick = { onNavigate(SettingsRoute.Input) },
            selected = currentRoute is SettingsRoute.Input,
            leadingContent = { Icon(painterResource(R.drawable.edit_24px), null) },
            content = { Text(stringResource(R.string.settings_category_input_title)) },
            supportingContent = { Text(stringResource(R.string.settings_category_input_subtitle)) }
        )
        ListItem(
            onClick = { onNavigate(SettingsRoute.Performance) },
            selected = currentRoute is SettingsRoute.Performance,
            leadingContent = { Icon(painterResource(R.drawable.speed_24px), null) },
            content = { Text(stringResource(R.string.settings_category_performance_title)) },
            supportingContent = { Text(stringResource(R.string.settings_category_performance_subtitle)) }
        )
        ListItem(
            onClick = { onNavigate(SettingsRoute.Miscellaneous) },
            selected = currentRoute is SettingsRoute.Miscellaneous,
            leadingContent = { Icon(painterResource(R.drawable.settings_24px), null) },
            content = { Text(stringResource(R.string.settings_category_miscellaneous_title)) },
            supportingContent = { Text(stringResource(R.string.settings_category_miscellaneous_subtitle)) }
        )
        ListItem(
            onClick = { onNavigate(SettingsRoute.About) },
            selected = currentRoute is SettingsRoute.About,
            leadingContent = { Icon(painterResource(R.drawable.info_24px), null) },
            content = { Text(stringResource(R.string.settings_category_about_title)) },
            supportingContent = { Text(stringResource(R.string.settings_category_about_subtitle)) }
        )
        VersionFooter(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp)
        )
    }
}

@Composable
private fun VersionFooter(modifier: Modifier = Modifier) {
    val version = stringResource(R.string.app_version)
    val git = stringResource(R.string.app_git_hash)

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(stringResource(R.string.app_name_full))
            Text(stringResource(R.string.settings_about_version_subtitle).format(version, git))

            Icon(
                modifier = Modifier.size(200.dp),
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = stringResource(R.string.app_name_full)
            )
        }
    }
}