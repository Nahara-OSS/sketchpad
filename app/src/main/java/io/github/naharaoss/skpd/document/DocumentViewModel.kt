package io.github.naharaoss.skpd.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.naharaoss.skpd.resource.LibraryRepository
import io.github.naharaoss.skpd.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DocumentViewModel.Factory::class)
class DocumentViewModel @AssistedInject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    @Assisted val documentRef: DocumentRef
) : ViewModel() {
    private val _document = MutableStateFlow<SketchpadDocumentV1?>(null)
    private val _activeLayer = MutableStateFlow<SketchpadDocumentV1.Layer?>(null)
    private val _layers = MutableStateFlow<List<SketchpadDocumentV1.Layer>>(emptyList())
    private val _changed = MutableSharedFlow<Unit>()
    val document: StateFlow<DocumentAccess?> = _document.asStateFlow()
    val activeLayer: StateFlow<DocumentAccess.Layer?> = _activeLayer.asStateFlow()
    val layers: StateFlow<List<DocumentAccess.Layer>> = _layers.asStateFlow()
    val changed = _changed.asSharedFlow()

    init {
        viewModelScope.launch {
            val document = documentRef.openDocument(this@DocumentViewModel)
            _document.value = document
            _activeLayer.value = document.activeLayer
            _layers.value = document.layers
            addCloseable(document)
        }
    }

    fun setActiveLayer(layer: DocumentAccess.Layer?) {
        if (layer !is SketchpadDocumentV1.Layer) throw Exception("Invalid layer implementation")
        _document.value?.activeLayer = layer
        _activeLayer.value = layer
    }

    fun addLayer() {
        val layer = (_document.value ?: return).addLayer()
        _layers.update { it + layer }
        setActiveLayer(layer)
    }

    fun deleteLayer(layer: DocumentAccess.Layer) {
        if (layer !is SketchpadDocumentV1.Layer) throw Exception("Invalid layer implementation")
        val document = _document.value ?: return
        layer.delete()
        _activeLayer.value = document.activeLayer
        _layers.value = document.layers
        viewModelScope.launch { _changed.emit(Unit) }
    }

    fun setLayerVisibility(layer: DocumentAccess.Layer, visible: Boolean) {
        if (layer !is SketchpadDocumentV1.Layer) throw Exception("Invalid layer implementation")
        layer.visible = visible
        viewModelScope.launch { _changed.emit(Unit) }
    }

    @AssistedFactory
    interface Factory {
        fun create(documentRef: DocumentRef): DocumentViewModel
    }

    sealed interface DocumentRef {
        suspend fun openDocument(vm: DocumentViewModel): SketchpadDocumentV1

        data class Local(val documentId: Long) : DocumentRef {
            override suspend fun openDocument(vm: DocumentViewModel): SketchpadDocumentV1 {
                val item = vm.libraryRepository.getDocumentById(documentId)
                return vm.libraryRepository.openDocument(item)
            }
        }
    }
}