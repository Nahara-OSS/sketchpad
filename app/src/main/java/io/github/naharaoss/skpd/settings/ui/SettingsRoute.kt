package io.github.naharaoss.skpd.settings.ui

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
sealed interface SettingsRoute : Parcelable {
    object Main : SettingsRoute
    object Appearance : SettingsRoute
    object Toolbars : SettingsRoute
    object Input : SettingsRoute
    object Performance : SettingsRoute
    object Miscellaneous : SettingsRoute
    object About : SettingsRoute
}