package io.github.naharaoss.skpd.document

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.naharaoss.skpd.settings.SettingsRepository
import io.github.naharaoss.skpd.utils.Size
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val document = MemoryDocument(
        tileSizeLog = settingsRepository.settings.value.performance.defaultTileSizeLog,
        size = Size.Infinite,
        background = Color.White
    )

    init {
        val layer = document.addLayer()
    }
}