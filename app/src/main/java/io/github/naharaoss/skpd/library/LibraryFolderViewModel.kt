package io.github.naharaoss.skpd.library

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.naharaoss.skpd.resource.LibraryItem
import io.github.naharaoss.skpd.resource.LibraryRepository
import io.github.naharaoss.skpd.utils.Size
import io.github.naharaoss.skpd.utils.prepend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = LibraryFolderViewModel.Factory::class)
class LibraryFolderViewModel @AssistedInject constructor(
    private val libraryRepository: LibraryRepository,
    @param:Assisted val folder: LibraryItem.Folder
) : ViewModel() {
    private val _content = MutableStateFlow<List<LibraryItem>?>(null)
    val content = _content.asStateFlow()

    init {
        viewModelScope.launch {
            val content = libraryRepository.getContent(folder).sortedByDescending { it.lastModified }
            _content.update { if (it != null) it + content else content }
        }

        viewModelScope.launch {
            libraryRepository.addition.collect { item ->
                _content.update { it?.prepend(item) ?: listOf(item) }
            }
        }

        viewModelScope.launch {
            libraryRepository.update.collect { item ->
                _content.update { it?.map { if (it.id == item.id) item else it }?.sortedByDescending { it.lastModified } }
            }
        }

        viewModelScope.launch {
            libraryRepository.removal.collect { item ->
                _content.update { it?.filter { entry -> entry.id != item.id } }
            }
        }
    }

    suspend fun createFolder(name: String) = libraryRepository.createFolder(folder, name)
    suspend fun createDocument(name: String, size: Size) = libraryRepository.createDocument(folder, name, size, Color.White)
    suspend fun renameItem(item: LibraryItem, newName: String) = libraryRepository.renameItem(item, newName)
    suspend fun deleteItem(item: LibraryItem) = libraryRepository.deleteItem(item)

    @AssistedFactory
    interface Factory {
        fun create(folder: LibraryItem.Folder): LibraryFolderViewModel
    }
}