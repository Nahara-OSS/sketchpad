package io.github.naharaoss.skpd.document

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.naharaoss.skpd.resource.LibraryRepository
import io.github.naharaoss.skpd.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DocumentViewModel.Factory::class)
class DocumentViewModel @AssistedInject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    @param:ApplicationContext private val context: Context,
    @Assisted val documentRef: DocumentRef
) : ViewModel() {
    private val _document = MutableStateFlow<SketchpadDocumentV1?>(null)
    private val _activeLayer = MutableStateFlow<SketchpadDocumentV1.Layer?>(null)
    val document: StateFlow<DocumentAccess?> = _document.asStateFlow()
    val activeLayer: StateFlow<DocumentAccess.Layer?> = _activeLayer.asStateFlow()

    init {
        viewModelScope.launch {
            val document = documentRef.openDocument(this@DocumentViewModel)
            _document.value = document
            _activeLayer.value = document.activeLayer
            addCloseable(document)
        }
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