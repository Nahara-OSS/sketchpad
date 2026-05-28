package io.github.naharaoss.skpd.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppearanceSettings(
    /**
     * Color scheme for entire app.
     *
     * Configure the color scheme used for entire app. [ColorScheme.Inherit] will use the system
     * color scheme.
     */
    val appColorScheme: ColorScheme = ColorScheme.Inherit,
) {
    enum class ColorScheme {
        Light,
        Dark,
        Inherit
    }
}
