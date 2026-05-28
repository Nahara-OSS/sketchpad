package io.github.naharaoss.skpd.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.naharaoss.skpd.settings.SettingsViewModel

@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    route: SettingsRoute,
    viewModel: SettingsViewModel,
    onNavigate: (SettingsRoute) -> Unit,
    onOpenSourceCode: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    when (route) {
        is SettingsRoute.Main -> MainSettingsPage(
            modifier = modifier,
            currentRoute = route,
            onNavigate = onNavigate
        )

        is SettingsRoute.Performance -> PerformanceSettingsPage(
            modifier = modifier,
            settings = settings.performance,
            onSettingsChange = { viewModel.changeSettings(settings.copy(performance = it)) }
        )

        is SettingsRoute.Miscellaneous -> MiscellaneousSettingsPage(
            modifier = modifier,
            settings = settings.miscellaneous,
            onSettingsChange = { viewModel.changeSettings(settings.copy(miscellaneous = it)) }
        )

        is SettingsRoute.About -> AboutSettingsPage(
            modifier = modifier,
            onOpenSourceCode = onOpenSourceCode
        )

        else -> Box(modifier)
    }
}