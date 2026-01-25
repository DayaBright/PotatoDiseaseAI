package com.tesis.potatodiseaseai.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.potatodiseaseai.data.tflite.ImageClassifierHelper
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
    val shouldNavigateToResult: Boolean = false
)

class ScannerViewModel(private val context: Context? = null) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var classifier: ImageClassifierHelper? = null

    init {
        context?.let {
            classifier = ImageClassifierHelper(it)
        }
    }

    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(flashEnabled = !_uiState.value.flashEnabled)
    }

    fun startCapture() {
        _uiState.value = _uiState.value.copy(isCapturing = true, error = null)
    }

    fun onCaptureSuccess(uri: Uri?) {
        _uiState.value = _uiState.value.copy(isCapturing = false, lastPhotoUri = uri)
        uri?.let { classifyImage(it) }
    }

    fun onCaptureError(message: String) {
        _uiState.value = _uiState.value.copy(isCapturing = false, error = message)
    }

    private fun classifyImage(uri: Uri) {
        val localContext = context
        val localClassifier = classifier

        if (localContext == null || localClassifier == null) {
            _uiState.value = _uiState.value.copy(error = "Clasificador no disponible")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                _uiState.value = _uiState.value.copy(isClassifying = true, error = null)

                val bitmap = loadBitmapFromUri(localContext, uri)
                val result = localClassifier.classify(bitmap)

                _uiState.value = _uiState.value.copy(
                    classification = result.label,
                    confidence = result.confidence,
                    isClassifying = false,
                    shouldNavigateToResult = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error: ${e.message}",
                    isClassifying = false
                )
            }
        }
    }

    fun onNavigatedToResult() {
        _uiState.value = _uiState.value.copy(shouldNavigateToResult = false)
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    override fun onCleared() {
        super.onCleared()
        classifier?.clear()
    }
}
