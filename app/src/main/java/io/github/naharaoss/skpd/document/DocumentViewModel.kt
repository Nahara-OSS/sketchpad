package io.github.naharaoss.skpd.document

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.naharaoss.skpd.settings.SettingsRepository
import io.github.naharaoss.skpd.utils.Size
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val _activeLayer = MutableStateFlow<SketchpadDocumentV1.Layer?>(null)
    val document: SketchpadDocumentV1
    val activeLayer get() = _activeLayer.asStateFlow()

    init {
        val fileId = "main.skpd"
        Log.d("DocumentViewModel", "File is $fileId")

        val file = File(context.cacheDir, fileId)
        val init = !file.exists()
        val channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)

        if (init) {
            document = SketchpadDocumentV1.init(
                channel = channel,
                tileSizeLog = settingsRepository.settings.value.performance.defaultTileSizeLog,
                size = Size.Infinite,
                background = Color.White
            )

            val layer = document.addLayer()
            document.activeLayer = layer
            _activeLayer.value = layer
        } else {
            document = SketchpadDocumentV1.load(channel)
            _activeLayer.value = document.activeLayer
        }

        addCloseable(document)
    }

    fun setActiveLayer(layer: SketchpadDocumentV1.Layer?) {
        document.activeLayer = layer
        _activeLayer.value = layer
    }
}