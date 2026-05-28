package io.github.naharaoss.skpd.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val input: InputSettings = InputSettings(),
    val performance: PerformanceSettings = PerformanceSettings(),
    val miscellaneous: MiscellaneousSettings = MiscellaneousSettings(),
)