package com.tesis.potatodiseaseai.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.potatodiseaseai.data.database.EnfermedadEntity
import com.tesis.potatodiseaseai.data.repository.AnalisisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiseaseDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val _enfermedad = MutableStateFlow<EnfermedadEntity?>(null)
    val enfermedad: StateFlow<EnfermedadEntity?> = _enfermedad
    private val repository = AnalisisRepository(application)


    fun loadEnfermedad(id: Long) {
        if (_enfermedad.value != null) return // Ya cargado
        viewModelScope.launch {
            _enfermedad.value = repository.getEnfermedadById(id)
        }
    }
}