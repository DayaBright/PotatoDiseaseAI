package com.tesis.potatodiseaseai.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.potatodiseaseai.data.database.AnalisisConEnfermedad
import com.tesis.potatodiseaseai.data.repository.AnalisisRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val analisis: List<AnalisisConEnfermedad> = emptyList(),
    val storageSize: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteDialog: AnalisisConEnfermedad? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalisisRepository(application)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadAnalisis()
    }

    private fun loadAnalisis() {
        viewModelScope.launch {
            repository.getAllAnalisis()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { lista ->
                    val size = repository.getTotalStorageSize()
                    _uiState.update {
                        it.copy(analisis = lista, storageSize = size, isLoading = false)
                    }
                }
        }
    }

    fun showDeleteDialog(analisis: AnalisisConEnfermedad) {
        _uiState.update { it.copy(showDeleteDialog = analisis) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = null) }
    }

    fun deleteAnalisis(analisis: AnalisisConEnfermedad) {
        viewModelScope.launch {
            val success = repository.deleteAnalisis(analisis)
            if (success) {
                dismissDeleteDialog()
            } else {
                _uiState.update { it.copy(error = "Error al eliminar el análisis") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}