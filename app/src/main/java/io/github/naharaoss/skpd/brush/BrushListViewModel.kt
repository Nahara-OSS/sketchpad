package io.github.naharaoss.skpd.brush

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.resource.BrushItem
import io.github.naharaoss.skpd.resource.BrushRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BrushListViewModel @Inject constructor(
    private val brushRepository: BrushRepository
): ViewModel() {
    private val _keyword = MutableStateFlow<String?>(null)
    private val _brushes = MutableStateFlow<List<BrushItem>?>(null)
    val keyword = _keyword.asStateFlow()
    val brushes = _brushes.asStateFlow()

    init {
        viewModelScope.launch {
            _brushes.value = brushRepository.getAll(null)
        }

        viewModelScope.launch {
            brushRepository.addition.collect { brush ->
                if (!filter(brush)) return@collect
                _brushes.update { if (it != null) (it + brush).sortedBy { it.name } else listOf(brush) }
            }
        }

        viewModelScope.launch {
            brushRepository.removal.collect { brush ->
                _brushes.update { it?.filter { it.id != brush.id } ?: emptyList() }
            }
        }

        viewModelScope.launch {
            brushRepository.update.collect { brush ->
                _brushes.update {
                    it
                        ?.map { if (it.id == brush.id) brush else it }
                        ?.filter(::filter)
                }
            }
        }
    }

    private fun filter(brush: BrushItem): Boolean {
        val keyword = _keyword.value

        if (keyword != null && !brush.name.contains(keyword)) return false
        return true
    }

    fun search(keyword: String?) {
        _keyword.value = keyword
        viewModelScope.launch { _brushes.value = brushRepository.getAll(_keyword.value) }
    }

    suspend fun getBrushById(id: Long) = brushRepository.getById(id)
    suspend fun createBrush(name: String, icon: String?) = brushRepository.createBrush(name, icon, StampBrush.defaultPreset)
}