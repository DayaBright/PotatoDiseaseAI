package com.tesis.potatodiseaseai.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.DetectionEntity
import com.tesis.potatodiseaseai.data.model.DiseaseDatabase
import com.tesis.potatodiseaseai.data.tflite.ImageClassifierHelper
import com.tesis.potatodiseaseai.utils.AppLogger
import com.tesis.potatodiseaseai.utils.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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
    
    private companion object {
        const val TAG = "ScannerViewModel"
    }

    // Se crea UNA SOLA VEZ cuando se necesita
    private val classifier: ImageClassifierHelper by lazy {
        ImageClassifierHelper(application.applicationContext)
    }
    
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

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

    /**
     * Pipeline simplificado: cargar → rotar → recortar 1:1 → clasificar + guardar.
     * UNA sola decodificación, UN solo guardado.
     */
    private fun classifyAndSave(sourceUri: Uri) {
        val localClassifier = classifier

        if (!localClassifier.isReady()) {
            _uiState.value = _uiState.value.copy(
                error = ErrorHandler.getUserMessage(
                    com.tesis.potatodiseaseai.utils.AppError.ClassificationError()
                )
            )
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var rawBitmap: Bitmap? = null
            var rotatedBitmap: Bitmap? = null
            var croppedBitmap: Bitmap? = null
            try {
                _uiState.value = _uiState.value.copy(isClassifying = true, error = null)
                val ctx = getApplication<Application>().applicationContext

                // ── PASO 1: Decodificar bitmap (UNA sola vez) ──
                rawBitmap = ctx.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: throw java.io.IOException("No se pudo decodificar la imagen")

                // ── PASO 2: Corregir rotación EXIF (en memoria, sin guardar) ──
                rotatedBitmap = fixRotationInMemory(ctx, sourceUri, rawBitmap)

                // ── PASO 3: Recortar cuadrado que coincide con la guía visual ──
                val guideFraction = 0.85f
                val displayMetrics = ctx.resources.displayMetrics
                val screenW = displayMetrics.widthPixels.toFloat()
                val screenH = displayMetrics.heightPixels.toFloat()
                val imgW = rotatedBitmap.width.toFloat()
                val imgH = rotatedBitmap.height.toFloat()

                // FILL_CENTER usa el factor de escala mayor para llenar toda la vista
                val fillScale = maxOf(screenW / imgW, screenH / imgH)

                // Tamaño de la guía en pantalla (px) → convertir a píxeles de imagen
                val guideScreenPx = minOf(screenW, screenH) * guideFraction
                val guideCamPx = (guideScreenPx / fillScale).toInt()
                    .coerceAtMost(minOf(rotatedBitmap.width, rotatedBitmap.height))

                val x = (rotatedBitmap.width - guideCamPx) / 2
                val y = (rotatedBitmap.height - guideCamPx) / 2
                croppedBitmap = Bitmap.createBitmap(rotatedBitmap, x, y, guideCamPx, guideCamPx)
                AppLogger.debug(TAG, "Imagen recortada: ${croppedBitmap.width}x${croppedBitmap.height}")

                // ── PASO 4: Clasificar ──
                val result = localClassifier.classify(croppedBitmap)

                // Validar que la clasificación fue exitosa
                if (result.error != null || result.label.isBlank()) {
                    throw Exception(result.error?.message ?: "Clasificación fallida")
                }

                // ── PASO 5: Guardar imagen recortada (UNA sola vez, directo) ──
                val directory = File(ctx.filesDir, "detections")
                if (!directory.exists()) directory.mkdirs()

                val filename = "IMG_${System.currentTimeMillis()}.jpg"
                val file = File(directory, filename)
                FileOutputStream(file).use { out ->
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val savedUri = Uri.fromFile(file)
                AppLogger.debug(TAG, "✓ Imagen guardada: ${file.absolutePath}")

                // ── PASO 6: Guardar en Room ──
                val detection = DetectionEntity(
                    imageUri = savedUri.toString(),
                    disease = result.label,
                    diseaseName = DiseaseDatabase.getDiseaseName(result.label),
                    confidence = result.confidence
                )
                val detectionId = database.detectionDao().insert(detection)

                // ── PASO 7: Actualizar UI ──
                _uiState.value = _uiState.value.copy(
                    lastPhotoUri = savedUri,
                    classification = result.label,
                    confidence = result.confidence,
                    isClassifying = false,
                    shouldNavigateToResult = true,
                    savedDetectionId = detectionId,
                    flashEnabled = false // La cámara se desvincula 
                )
            } catch (e: Exception) {
                val appError = ErrorHandler.handleException(e, "Clasificación y guardado")
                _uiState.value = _uiState.value.copy(
                    error = ErrorHandler.getUserMessage(appError),
                    isClassifying = false
                )
            } finally {
                // Reciclar bitmaps (solo los que son objetos distintos)
                if (croppedBitmap != null && croppedBitmap !== rotatedBitmap) croppedBitmap.recycle()
                if (rotatedBitmap != null && rotatedBitmap !== rawBitmap) rotatedBitmap.recycle()
                rawBitmap?.recycle()

                // Limpiar archivo temporal de captura
                try {
                    val sourceFile = File(sourceUri.path ?: "")
                    if (sourceFile.exists() && sourceFile.name.startsWith("temp_")) {
                        sourceFile.delete()
                    }
                } catch (_: Exception) { }
            }
        }
    }

    /**
     * Corrige la rotación del bitmap en memoria según EXIF.
     */
    private fun fixRotationInMemory(context: android.content.Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it)
            }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            ) ?: ExifInterface.ORIENTATION_UNDEFINED

            val angle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return bitmap // sin rotación → devuelve el original
            }

            val matrix = Matrix().apply { postRotate(angle) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            AppLogger.error(TAG, "Error leyendo EXIF: ${e.message}")
            bitmap
        }
    }

    fun onNavigatedToResult() {
        _uiState.value = _uiState.value.copy(shouldNavigateToResult = false)
    }

    override fun onCleared() {
        super.onCleared()
        classifier.clear()
    }
}

