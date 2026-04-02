package com.tesis.potatodiseaseai.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.potatodiseaseai.data.database.DetectionEntity
import com.tesis.potatodiseaseai.data.repository.DetectionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val detections: List<DetectionEntity> = emptyList(),
    val storageSize: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteDialog: DetectionEntity? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = DetectionRepository(application)
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadDetections()
    }
    
    private fun loadDetections() {
        viewModelScope.launch {
            repository.getAllDetections()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { detections ->
                    // Recalcular almacenamiento cada vez que cambia la lista
                    // (nuevo análisis, eliminación desde cualquier pantalla)
                    val size = repository.getTotalStorageSize()
                    _uiState.update { 
                        it.copy(
                            detections = detections,
                            storageSize = size,
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    fun showDeleteDialog(detection: DetectionEntity) {
        _uiState.update { it.copy(showDeleteDialog = detection) }
    }
    
    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = null) }
    }
    
    fun deleteDetection(detection: DetectionEntity) {
        viewModelScope.launch {
            val success = repository.deleteDetection(detection)
            if (success) {
                dismissDeleteDialog()
                // storageSize se recalcula automáticamente en el collector del Flow
            } else {
                _uiState.update { 
                    it.copy(error = "Error al eliminar la detección") 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}