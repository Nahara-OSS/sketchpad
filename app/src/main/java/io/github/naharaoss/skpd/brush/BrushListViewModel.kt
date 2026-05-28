package io.github.naharaoss.skpd.brush

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.naharaoss.skpd.brush.impl.StampBrush
import io.github.naharaoss.skpd.resource.BrushRepository
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class BrushListViewModel @Inject constructor(
    private val resourceRepository: BrushRepository
): ViewModel() {
    val brushes = resourceRepository.brushes

    init {
        viewModelScope.launch {
            Log.d("BrushListViewModel", "Getting brushes...")
            val brushes = resourceRepository.getBrushes()
            Log.d("BrushListViewModel", "$brushes")
        }
    }

    suspend fun createBrush(name: String, icon: String?) = resourceRepository.createBrush(name, icon, StampBrush.defaultPreset)
}