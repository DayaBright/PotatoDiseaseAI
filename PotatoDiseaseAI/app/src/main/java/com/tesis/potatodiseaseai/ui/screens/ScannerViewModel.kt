package com.tesis.potatodiseaseai.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.potatodiseaseai.data.repository.AnalisisRepository
import com.tesis.potatodiseaseai.data.tflite.ImageClassifierHelper
import com.tesis.potatodiseaseai.utils.AppLogger
import com.tesis.potatodiseaseai.utils.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.camera.core.ImageProxy
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
    val savedDetectionId: Long? = null,
    val liveClassification: String? = null,
    val isLiveLowConfidence: Boolean = false
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

    private val repository = AnalisisRepository(application.applicationContext)

    @Volatile
    private var diseaseNamesMap = emptyMap<String, String>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllEnfermedades().collect { list ->
                val newMap = mutableMapOf<String, String>()
                list.forEach { newMap[it.labelCnn] = it.nombre }
                diseaseNamesMap = newMap
            }
        }
    }

    fun toggleFlash() {
        _uiState.value = _uiState.value.copy(flashEnabled = !_uiState.value.flashEnabled)
    }

    fun turnOffFlash() {
        if (_uiState.value.flashEnabled) {
            _uiState.value = _uiState.value.copy(flashEnabled = false)
        }
    }

    private var lastAnalysisTime = 0L

    fun analyzeFrame(imageProxy: ImageProxy) {
        val localClassifier = classifier
        if (!localClassifier.isReady() || _uiState.value.isClassifying || _uiState.value.isCapturing) {
            imageProxy.close()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < 2000) {
            imageProxy.close()
            return
        }
        lastAnalysisTime = currentTime

        viewModelScope.launch(Dispatchers.IO) {
            var rawBitmap: Bitmap? = null
            try {
                val ctx = getApplication<Application>().applicationContext
                rawBitmap = imageProxy.toBitmap()

                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                
                // La rotación y el recorte ahora los maneja el ImageProcessor
                val result = localClassifier.classify(rawBitmap, rotationDegrees)
                
                if (result.error == null && result.label.isNotBlank()) {
                    // El LabelNormalizer convierte "z_no_potato" → "z no potato" (reemplaza _ por espacios)
                    val normalizedLabel = com.tesis.potatodiseaseai.utils.LabelNormalizer.normalize(result.label)
                    val isNoPotatoClass = normalizedLabel == "z no potato"
                    val isLowConfidence = result.confidence < 0.70f || isNoPotatoClass
                    val translatedName = diseaseNamesMap[normalizedLabel] ?: result.label
                    _uiState.value = _uiState.value.copy(
                        liveClassification = if (isLowConfidence) null else translatedName,
                        isLiveLowConfidence = isLowConfidence
                    )
                }
            } catch (e: Exception) {
                AppLogger.error(TAG, "Error en live analysis: ${e.message}")
            } finally {
                rawBitmap?.recycle()
                imageProxy.close()
            }
        }
    }

    private var lastLiveLowConfidence = false

    fun startCapture() {
        lastLiveLowConfidence = _uiState.value.isLiveLowConfidence
        _uiState.value = _uiState.value.copy(isCapturing = true, error = null)
    }

    fun onCaptureSuccess(uri: Uri?) {
        _uiState.value = _uiState.value.copy(isCapturing = false, lastPhotoUri = uri)
        uri?.let { classifyAndSave(it) }
    }

    /**
     * Usa letterbox en lugar de recorte para conservar toda la información.
     */
    fun onGalleryImageSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(isCapturing = false, lastPhotoUri = uri)
        classifyAndSaveFromGallery(uri)
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
     * Pipeline: cargar → rotar → recortar 1:1 → clasificar + guardar.
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

                // ── PASO 2: Extraer orientación EXIF (sin rotar en memoria) ──
                var rotationDegrees = 0
                ctx.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                    rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }

                // ── PASO 3: Clasificar ──
                // ImageClassifierHelper manejará internamente la rotación y el recorte usando ImageProcessor (C++)
                val result = localClassifier.classify(rawBitmap, rotationDegrees)

                // Validar que la clasificación fue exitosa
                if (result.error != null || result.label.isBlank()) {
                    throw Exception(result.error?.message ?: "Clasificación fallida")
                }

                // Para guardar la imagen: aplicamos la rotación y el recorte
                rotatedBitmap = com.tesis.potatodiseaseai.utils.ImageUtils.fixRotationInMemory(ctx, sourceUri, rawBitmap)
                
                // Mapear el recorte exactamente al recuadro visual de la cámara
                val displayMetrics = ctx.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                val density = displayMetrics.density
                
                croppedBitmap = com.tesis.potatodiseaseai.utils.ImageUtils.cropToPreviewSquare(
                    source = rotatedBitmap,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    guideFraction = 0.85f,
                    verticalOffsetDp = 60f,
                    density = density
                )

                val isNoPotatoClass = result.label == "z no potato"
                // Si en vivo decía "No se detecta hoja", forzamos que la captura también
                // para evitar que el modelo asigne una enfermedad errónea por el cambio de resolución.
                val isLowConfidence = lastLiveLowConfidence || result.confidence < 0.70f || isNoPotatoClass

                val savedUri: Uri
                val detectionId: Long?

                if (isLowConfidence) {
                    // ── Confianza baja: guardar imagen temporal solo para mostrar en ResultScreen ──
                    val tempDir = File(ctx.cacheDir, "temp_detections")
                    if (!tempDir.exists()) tempDir.mkdirs()
                    val tempFile = File(tempDir, "TEMP_${System.currentTimeMillis()}.webp")
                    FileOutputStream(tempFile).use { out ->
                        croppedBitmap?.compress(Bitmap.CompressFormat.WEBP, 80, out)
                    }
                    savedUri = Uri.fromFile(tempFile)
                    detectionId = null
                    AppLogger.debug(TAG, "⚠ Confianza baja (${result.confidence}) — NO guardado en historial")
                } else {
                    // ── PASO 5: Guardar imagen recortada (UNA sola vez, directo) ──
                    val directory = File(ctx.filesDir, "detections")
                    if (!directory.exists()) directory.mkdirs()
                    val filename = "IMG_${System.currentTimeMillis()}.webp"
                    val file = File(directory, filename)
                    FileOutputStream(file).use { out ->
                        croppedBitmap?.compress(Bitmap.CompressFormat.WEBP, 80, out)
                    }
                    savedUri = Uri.fromFile(file)
                    AppLogger.debug(TAG, "✓ Imagen guardada: ${file.absolutePath}")

                    // ── PASO 6: Guardar en Room ──
                    detectionId = repository.insertAnalisis(
                        labelCnn = result.label,
                        imagenUri = savedUri.toString(),
                        precision = result.confidence
                    )
                }

                // ── PASO 7: Actualizar UI ──
                // Usamos "z no potato" (normalizado) para que ResultScreen lo detecte correctamente
                _uiState.value = _uiState.value.copy(
                    lastPhotoUri = savedUri,
                    classification = if (isLowConfidence) "z no potato" else result.label,
                    confidence = if (isNoPotatoClass) result.confidence else if (isLowConfidence) 0f else result.confidence,
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
     * Pipeline para imágenes de galería: cargar → rotar → letterbox 224×224 → clasificar + guardar.
     */
    private fun classifyAndSaveFromGallery(sourceUri: Uri) {
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
            var processedBitmap: Bitmap? = null
            try {
                _uiState.value = _uiState.value.copy(isClassifying = true, error = null)
                val ctx = getApplication<Application>().applicationContext

                // ── PASO 1: Decodificar bitmap ──
                rawBitmap = ctx.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: throw java.io.IOException("No se pudo decodificar la imagen")

                // ── PASO 2: Extraer orientación EXIF ──
                var rotationDegrees = 0
                ctx.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                    rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }

                // La clasificación usa el procesador C++ de TF Lite
                // Pasarle la imagen original y la rotación es suficiente
                val result = localClassifier.classify(rawBitmap, rotationDegrees)

                if (result.error != null || result.label.isBlank()) {
                    throw Exception(result.error?.message ?: "Clasificación fallida")
                }

                // Para guardar en historial: rotamos y recortamos para la UI de Android usando ImageUtils centralizado
                processedBitmap = com.tesis.potatodiseaseai.utils.ImageUtils.fixRotationInMemory(ctx, sourceUri, rawBitmap)
                val targetSize = maxOf(processedBitmap.width, processedBitmap.height).coerceAtMost(1024)
                val savedBitmap = com.tesis.potatodiseaseai.utils.ImageUtils.letterboxBitmap(processedBitmap, targetSize)

                // El LabelNormalizer ya normalizó el label: "z_no_potato" → "z no potato"
                val isNoPotatoClass = result.label == "z no potato"
                val isLowConfidence = result.confidence < 0.70f || isNoPotatoClass

                val savedUri: Uri
                val detectionId: Long?

                if (isLowConfidence) {
                    val tempDir = File(ctx.cacheDir, "temp_detections")
                    if (!tempDir.exists()) tempDir.mkdirs()
                    val tempFile = File(tempDir, "TEMP_${System.currentTimeMillis()}.webp")
                    FileOutputStream(tempFile).use { out ->
                        savedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                    }
                    savedUri = Uri.fromFile(tempFile)
                    detectionId = null
                    AppLogger.debug(TAG, "⚠ Confianza baja (${result.confidence}) — NO guardado en historial")
                } else {
                    val directory = File(ctx.filesDir, "detections")
                    if (!directory.exists()) directory.mkdirs()
                    val filename = "IMG_${System.currentTimeMillis()}.webp"
                    val file = File(directory, filename)
                    FileOutputStream(file).use { out ->
                        savedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                    }
                    savedUri = Uri.fromFile(file)
                    AppLogger.debug(TAG, "✓ Imagen guardada: ${file.absolutePath}")

                    detectionId = repository.insertAnalisis(
                        labelCnn = result.label,
                        imagenUri = savedUri.toString(),
                        precision = result.confidence
                    )
                }

                if (savedBitmap !== processedBitmap) savedBitmap.recycle()

                // Usamos "z no potato" (normalizado) para que ResultScreen lo detecte correctamente
                _uiState.value = _uiState.value.copy(
                    lastPhotoUri = savedUri,
                    classification = if (isLowConfidence) "z no potato" else result.label,
                    confidence = if (isNoPotatoClass) result.confidence else if (isLowConfidence) 0f else result.confidence,
                    isClassifying = false,
                    shouldNavigateToResult = true,
                    savedDetectionId = detectionId,
                    flashEnabled = false
                )
            } catch (e: Exception) {
                val appError = ErrorHandler.handleException(e, "Clasificación y guardado (galería)")
                _uiState.value = _uiState.value.copy(
                    error = ErrorHandler.getUserMessage(appError),
                    isClassifying = false
                )
            } finally {
                if (processedBitmap != null && processedBitmap !== rawBitmap) processedBitmap.recycle()
                rawBitmap?.recycle()
            }
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

