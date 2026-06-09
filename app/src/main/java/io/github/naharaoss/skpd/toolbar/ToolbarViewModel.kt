package io.github.naharaoss.skpd.toolbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ToolbarViewModel @Inject constructor(
    private val toolbarRepository: ToolbarRepository
) : ViewModel() {
    val toolbars = toolbarRepository.toolbars

    fun changeToolbars(toolbars: List<Toolbar>) = viewModelScope.launch {
        toolbarRepository.writeToolbars(toolbars)
    }
}