package io.github.naharaoss.skpd.settings.ui

import android.util.Log
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

data class SettingsBackStack(
    val backStack: List<SettingsRoute>,
    val goBack: () -> Unit,
    val navigateTo: (route: SettingsRoute, isInMain: Boolean) -> Unit
)

@Composable
fun rememberSettingsBackStack(windowSizeClass: WindowSizeClass, onExit: () -> Unit): SettingsBackStack {
    var internalBackStack by rememberSaveable { mutableStateOf(emptyList<SettingsRoute>()) } // never contains main
    val singlePane = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    Log.i("Settings", "$internalBackStack");

    return SettingsBackStack(
        backStack = when {
            singlePane -> listOf(SettingsRoute.Main) + internalBackStack
            internalBackStack.isEmpty() -> listOf(SettingsRoute.Appearance)
            else -> internalBackStack
        },
        goBack = {
            Log.i("Settings", "GO BACK")
            when {
                internalBackStack.size > 1 -> internalBackStack = internalBackStack.dropLast(1)
                singlePane && internalBackStack.isNotEmpty() -> internalBackStack = internalBackStack.dropLast(1)
                else -> onExit()
            }
        },
        navigateTo = { route, isInMain ->
            internalBackStack = when {
                isInMain -> listOf(route)
                else -> internalBackStack + route
            }
        }
    )
}