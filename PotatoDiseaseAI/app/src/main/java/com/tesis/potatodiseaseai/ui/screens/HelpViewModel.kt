package com.tesis.potatodiseaseai.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.EnfermedadEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HelpUiState(
    val enfermedades: List<EnfermedadEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class HelpViewModel(application: Application) : AndroidViewModel(application) {

    private val enfermedadDao = AppDatabase.getDatabase(application).enfermedadDao()

    private val _uiState = MutableStateFlow(HelpUiState())
    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

    init {
        loadEnfermedades()
    }

    private fun loadEnfermedades() {
        viewModelScope.launch {
            enfermedadDao.getAllEnfermedades()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { lista ->
                    _uiState.update {
                        it.copy(enfermedades = lista, isLoading = false)
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
