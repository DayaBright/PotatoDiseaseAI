package com.tesis.potatodiseaseai.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.DetectionEntity
import com.tesis.potatodiseaseai.data.model.DiseaseDatabase
import com.tesis.potatodiseaseai.data.tflite.ImageClassifierHelper
import com.tesis.potatodiseaseai.utils.ErrorHandler
import com.tesis.potatodiseaseai.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(
    val flashEnabled: Boolean = false,
    val isCapturing: Boolean = false,
    val lastPhotoUri: Uri? = null,
    val error: String? = null,
    val classification: String? = null,
    val confidence: Float? = null,
    val isClassifying: Boolean = false,
    val shouldNavigateToResult: Boolean = false,
    val savedDetectionId: Long? = null
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var classifier: ImageClassifierHelper? = ImageClassifierHelper(application.applicationContext)
    private val database = AppDatabase.getDatabase(application.applicationContext)

    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(flashEnabled = !_uiState.value.flashEnabled)
    }

    fun startCapture() {
        _uiState.value = _uiState.value.copy(isCapturing = true, error = null)
    }

    fun onCaptureSuccess(uri: Uri?) {
        _uiState.value = _uiState.value.copy(isCapturing = false, lastPhotoUri = uri)
        uri?.let { classifyAndSave(it) }
    }

    fun onCaptureError(message: String) {
        _uiState.value = _uiState.value.copy(
            isCapturing = false,
            error = ErrorHandler.getUserMessage(
                ErrorHandler.handleException(
                    Exception(message),
                    "Captura de imagen"
                )
            )
        )
    }

    private fun classifyAndSave(sourceUri: Uri) {
        val localClassifier = classifier

        if (localClassifier == null || !localClassifier.isReady()) {
            _uiState.value = _uiState.value.copy(
                error = ErrorHandler.getUserMessage(
                    com.tesis.potatodiseaseai.utils.AppError.ClassificationError()
                )
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            try {
                _uiState.value = _uiState.value.copy(isClassifying = true, error = null)

                // 1. Clasificar la imagen
                bitmap = loadBitmapFromUri(sourceUri)
                val result = localClassifier.classify(bitmap)

                // 2. Guardar imagen en almacenamiento interno
                val savedUri = FileUtils.saveImageToInternalStorage(
                    getApplication<Application>().applicationContext,
                    sourceUri
                )
                
                if (savedUri == null) {
                    throw java.io.IOException("No se pudo guardar la imagen")
                }

                // 3. Guardar en Room
                val detection = DetectionEntity(
                    imageUri = savedUri.toString(),
                    disease = result.label,
                    diseaseName = DiseaseDatabase.getDiseaseName(result.label),
                    confidence = result.confidence
                )
                
                val detectionId = database.detectionDao().insert(detection)

                // 4. Actualizar UI y navegar
                _uiState.value = _uiState.value.copy(
                    lastPhotoUri = savedUri,
                    classification = result.label,
                    confidence = result.confidence,
                    isClassifying = false,
                    shouldNavigateToResult = true,
                    savedDetectionId = detectionId
                )
            } catch (e: Exception) {
                val appError = ErrorHandler.handleException(e, "Clasificación y guardado")
                _uiState.value = _uiState.value.copy(
                    error = ErrorHandler.getUserMessage(appError),
                    isClassifying = false
                )
            } finally {
                bitmap?.recycle()
            }
        }
    }

    fun onNavigatedToResult() {
        _uiState.value = _uiState.value.copy(shouldNavigateToResult = false)
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream).also {
                inputStream?.close()
            }
        } catch (e: Exception) {
            throw java.io.IOException("Error al cargar imagen: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        classifier?.clear()
        classifier = null
    }
}
