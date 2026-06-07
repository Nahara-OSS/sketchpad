package io.github.naharaoss.skpd.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.naharaoss.skpd.document.ui.DocumentViewInterface
import io.github.naharaoss.skpd.resource.BrushItem
import io.github.naharaoss.skpd.resource.BrushRepository
import io.github.naharaoss.skpd.resource.LibraryRepository
import io.github.naharaoss.skpd.settings.SettingsRepository
import io.github.naharaoss.skpd.utils.BlendMode
import io.github.naharaoss.skpd.utils.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DocumentViewModel.Factory::class)
class DocumentViewModel @AssistedInject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val brushRepository: BrushRepository,
    @Assisted val documentRef: DocumentRef
) : ViewModel() {
    private var document: SketchpadDocumentV1? = null
    private var view: DocumentViewInterface? = null
    private val _layers = MutableStateFlow<List<LayerRef>>(emptyList())
    private val _activeLayer = MutableStateFlow<LayerRef?>(null)
    private val _brush = settingsRepository.settings
        .map { it.session.selectedBrushId }
        .distinctUntilChanged()
        .map { if (it != null) brushRepository.getById(it) else null }
    private val _brushPreset = _brush
        .map { if (it != null) brushRepository.getPreset(it) else null }
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)
    val layers = _layers.asStateFlow()
    val activeLayer = _activeLayer.asStateFlow()
    val brush = _brush.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)
    val brushColor = settingsRepository.settings
        .map { it.session.selectedBrushColor }
        .distinctUntilChanged()
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = Color.Rgb(0f, 0f, 0f))

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { setting ->
                view?.fingerDrawing = setting.input.fingerDrawing
            }
        }

        viewModelScope.launch {
            _brushPreset.collect {
                view?.brushPreset = it
            }
        }

        viewModelScope.launch {
            brushColor.collect {
                view?.brushColor = it.toAndroidx()
            }
        }

        viewModelScope.launch {
            val document = documentRef.openDocument(this@DocumentViewModel)
            this@DocumentViewModel.document = document
            _layers.value = document.layers.map(::LayerRef)
            _activeLayer.value = document.activeLayer?.let(::LayerRef)
            view?.document = document
            view?.layer = document.activeLayer
            view?.fingerDrawing = settingsRepository.settings.value.input.fingerDrawing
            view?.brushPreset = _brushPreset.value
            view?.brushColor = brushColor.value.toAndroidx()
            view?.triggerDocumentUpdate()
            addCloseable(document)
        }
    }

    fun setActiveLayer(layer: LayerRef) {
        document?.let { document ->
            val layer = document.layers.find { it.id == layer.id }
            document.activeLayer = layer
            view?.layer = layer
        }

        _activeLayer.value = layer
    }

    fun addLayer(
        name: String = "Layer ${_layers.value.size + 1}",
        visible: Boolean = true,
        opacity: Float = 1f,
        blend: BlendMode = BlendMode.SourceOver
    ): LayerRef {
        return document?.let { document ->
            val layer = document.addLayer(
                name = name,
                visible = visible,
                opacity = opacity,
                blend = blend
            )

            document.activeLayer = layer
            view?.layer = layer
            view?.triggerDocumentUpdate()

            val ref = LayerRef(layer)
            _activeLayer.value = ref
            _layers.update { it + ref }
            ref
        } ?: throw Exception("Document is not available yet")
    }

    fun editLayer(
        layer: LayerRef,
        name: String = layer.name,
        visible: Boolean = layer.visible,
        opacity: Float = layer.opacity,
        blend: BlendMode = layer.blend
    ): LayerRef {
        document?.let { document ->
            document.layers.find { it.id == layer.id }?.let {
                if (it.name != name) it.name = name
                if (it.visible != visible) it.visible = visible
                if (it.opacity != opacity) it.opacity = opacity
                if (it.blend != blend) it.blend = blend
                view?.triggerDocumentUpdate()
            }
        }

        val layer = layer.copy(name = name, visible = visible, opacity = opacity, blend = blend)
        _layers.update { it.map { if (it.id == layer.id) layer else it } }
        return layer
    }

    fun deleteLayer(layer: LayerRef) {
        document?.let { document ->
            document.layers.find { it.id == layer.id }?.delete()
            view?.layer = document.activeLayer
            view?.triggerDocumentUpdate()
        }

        _activeLayer.value = document?.activeLayer?.let(::LayerRef)
        _layers.update { it.filter { it.id != layer.id } }
    }

    fun setBrush(brush: BrushItem?) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(session = it.session.copy(selectedBrushId = brush?.id)) }
        }
    }

    fun setBrushColor(color: Color) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(session = it.session.copy(selectedBrushColor = color)) }
        }
    }

    fun setView(view: DocumentViewInterface) {
        if (this.view == view) return
        this.view = view
        view.document = document
        view.layer = document?.activeLayer
        view.fingerDrawing = settingsRepository.settings.value.input.fingerDrawing
        view.brushPreset = _brushPreset.value
        view.brushColor = brushColor.value.toAndroidx()
    }

    @AssistedFactory
    interface Factory {
        fun create(documentRef: DocumentRef): DocumentViewModel
    }

    data class LayerRef(
        val id: Any,
        val name: String,
        val visible: Boolean,
        val opacity: Float,
        val blend: BlendMode
    ) {
        constructor(layer: DocumentAccess.Layer) : this(
            id = layer.id,
            name = layer.name,
            visible = layer.visible,
            opacity = layer.opacity,
            blend = layer.blend
        )
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