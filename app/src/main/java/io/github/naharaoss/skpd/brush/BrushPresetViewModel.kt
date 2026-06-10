package io.github.naharaoss.skpd.brush

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.naharaoss.skpd.resource.BrushItem
import io.github.naharaoss.skpd.resource.BrushRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = BrushPresetViewModel.Factory::class)
class BrushPresetViewModel @AssistedInject constructor(
    private val brushRepository: BrushRepository,
    @Assisted brush: BrushItem
) : ViewModel() {
    private val _brush = MutableStateFlow(brush)
    private val _preset = MutableStateFlow<BrushType.Preset?>(null)
    val brush = _brush.asStateFlow()
    val preset = _preset.asStateFlow()

    init {
        viewModelScope.launch {
            _preset.value = brushRepository.getPreset(brush)
        }

        viewModelScope.launch {
            brushRepository.update.collect {
                if (it.id == brush.id) _brush.value = it
            }
        }

        viewModelScope.launch {
            brushRepository.presetUpdate.collect {
                if (it.id != brush.id) return@collect
                _preset.value = brushRepository.getPreset(it)
            }
        }
    }

    suspend fun updatePreset(updater: (BrushType.Preset) -> BrushType.Preset) {
        _preset.update { if (it != null) updater(it) else it }
        _preset.value?.let { brushRepository.changePreset(_brush.value, it) }
    }

    @AssistedFactory
    interface Factory {
        fun create(brush: BrushItem): BrushPresetViewModel
    }
}