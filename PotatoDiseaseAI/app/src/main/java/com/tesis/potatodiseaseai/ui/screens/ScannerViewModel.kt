package com.tesis.potatodiseaseai.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

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
            var rotatedBitmap: Bitmap? = null
            var croppedBitmap: Bitmap? = null
            try {
                val ctx = getApplication<Application>().applicationContext
                rawBitmap = imageProxy.toBitmap()

                val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
                rotatedBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                
                val guideFraction = 0.85f
                val displayMetrics = ctx.resources.displayMetrics
                val screenW = displayMetrics.widthPixels.toFloat()
                val screenH = displayMetrics.heightPixels.toFloat()
                val imgW = rotatedBitmap.width.toFloat()
                val imgH = rotatedBitmap.height.toFloat()

                val fillScale = maxOf(screenW / imgW, screenH / imgH)

                val guideScreenPx = minOf(screenW, screenH) * guideFraction
                val guideCamPx = (guideScreenPx / fillScale).toInt()
                    .coerceAtMost(minOf(rotatedBitmap.width, rotatedBitmap.height))

                val verticalOffsetDp = 60f
                val verticalOffsetPx = verticalOffsetDp * displayMetrics.density
                val verticalOffsetCamPx = (verticalOffsetPx / fillScale).toInt()

                val x = (rotatedBitmap.width - guideCamPx) / 2
                val y = (rotatedBitmap.height - guideCamPx) / 2 - verticalOffsetCamPx
                val safeY = y.coerceIn(0, rotatedBitmap.height - guideCamPx)
                croppedBitmap = Bitmap.createBitmap(rotatedBitmap, x, safeY, guideCamPx, guideCamPx)

                val result = localClassifier.classify(croppedBitmap)
                
                if (result.error == null && result.label.isNotBlank()) {
                    // El LabelNormalizer convierte "z_no_potato" → "z no potato" (reemplaza _ por espacios)
                    // Por eso la comparación debe hacerse DESPUÉS de normalizar
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
                if (croppedBitmap != null && croppedBitmap !== rotatedBitmap) croppedBitmap.recycle()
                if (rotatedBitmap != null && rotatedBitmap !== rawBitmap) rotatedBitmap.recycle()
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
     * Punto de entrada para imágenes seleccionadas desde la galería.
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

                val verticalOffsetDp = 60f
                val verticalOffsetPx = verticalOffsetDp * displayMetrics.density
                val verticalOffsetCamPx = (verticalOffsetPx / fillScale).toInt()

                val x = (rotatedBitmap.width - guideCamPx) / 2
                val y = (rotatedBitmap.height - guideCamPx) / 2 - verticalOffsetCamPx
                val safeY = y.coerceIn(0, rotatedBitmap.height - guideCamPx)
                croppedBitmap = Bitmap.createBitmap(rotatedBitmap, x, safeY, guideCamPx, guideCamPx)
                AppLogger.debug(TAG, "Imagen recortada: ${croppedBitmap.width}x${croppedBitmap.height}")

                // ── PASO 4: Clasificar ──
                val result = localClassifier.classify(croppedBitmap)

                // Validar que la clasificación fue exitosa
                if (result.error != null || result.label.isBlank()) {
                    throw Exception(result.error?.message ?: "Clasificación fallida")
                }

                // El LabelNormalizer ya normalizó el label: "z_no_potato" → "z no potato"
                val isNoPotatoClass = result.label == "z no potato"
                // Si en vivo decía "No se detecta hoja", forzamos que la captura también lo sea
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
                        croppedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
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
                        croppedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
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
                    confidence = if (isLowConfidence) 0f else result.confidence,
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

    /**
     * Pipeline para imágenes de galería: cargar → rotar → letterbox 224×224 → clasificar + guardar.
     * No recorta: redimensiona la imagen completa dentro de 224×224 con padding negro.
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
            var rotatedBitmap: Bitmap? = null
            var letterboxedBitmap: Bitmap? = null
            try {
                _uiState.value = _uiState.value.copy(isClassifying = true, error = null)
                val ctx = getApplication<Application>().applicationContext

                // ── PASO 1: Decodificar bitmap ──
                rawBitmap = ctx.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: throw java.io.IOException("No se pudo decodificar la imagen")

                // ── PASO 2: Corregir rotación EXIF ──
                rotatedBitmap = fixRotationInMemory(ctx, sourceUri, rawBitmap)

                // ── PASO 3: Letterbox a un tamaño adecuado para la UI (sin recorte, sin distorsión) ──
                // Usamos un tamaño mayor para evitar que la imagen se vea borrosa en la pantalla de resultados.
                // ImageClassifierHelper redimensionará internamente la imagen al tamaño requerido por el modelo.
                val targetSize = maxOf(rotatedBitmap.width, rotatedBitmap.height).coerceAtMost(1024)
                letterboxedBitmap = letterboxBitmap(rotatedBitmap, targetSize)
                AppLogger.debug(TAG, "Imagen letterbox: ${letterboxedBitmap.width}x${letterboxedBitmap.height} (original: ${rotatedBitmap.width}x${rotatedBitmap.height})")

                // ── PASO 4: Clasificar ──
                val result = localClassifier.classify(letterboxedBitmap)

                if (result.error != null || result.label.isBlank()) {
                    throw Exception(result.error?.message ?: "Clasificación fallida")
                }

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
                        letterboxedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
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
                        letterboxedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                    }
                    savedUri = Uri.fromFile(file)
                    AppLogger.debug(TAG, "✓ Imagen guardada: ${file.absolutePath}")

                    detectionId = repository.insertAnalisis(
                        labelCnn = result.label,
                        imagenUri = savedUri.toString(),
                        precision = result.confidence
                    )
                }

                // Usamos "z no potato" (normalizado) para que ResultScreen lo detecte correctamente
                _uiState.value = _uiState.value.copy(
                    lastPhotoUri = savedUri,
                    classification = if (isLowConfidence) "z no potato" else result.label,
                    confidence = if (isLowConfidence) 0f else result.confidence,
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
                if (letterboxedBitmap != null && letterboxedBitmap !== rotatedBitmap) letterboxedBitmap.recycle()
                if (rotatedBitmap != null && rotatedBitmap !== rawBitmap) rotatedBitmap.recycle()
                rawBitmap?.recycle()
            }
        }
    }

    /**
     * Redimensiona un bitmap para que quepa dentro de un cuadrado de [targetSize]×[targetSize]
     * manteniendo la proporción original, y rellena los bordes vacíos con negro.
     *
     * Ejemplo: una imagen 640×480 se escala a 224×168 y se centra en un canvas 224×224
     * con 28px de padding negro arriba y abajo.
     */
    private fun letterboxBitmap(source: Bitmap, targetSize: Int): Bitmap {
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()

        // Factor de escala para que el lado mayor quepa en targetSize
        val scale = targetSize.toFloat() / maxOf(srcW, srcH)

        val scaledW = (srcW * scale).toInt()
        val scaledH = (srcH * scale).toInt()

        // Crear bitmap negro de targetSize×targetSize
        val output = createBitmap(targetSize, targetSize)
        val canvas = Canvas(output)
        canvas.drawColor(Color.rgb(112, 122, 95))

        // Centrar la imagen escalada
        val offsetX = (targetSize - scaledW) / 2f
        val offsetY = (targetSize - scaledH) / 2f

        val scaledBitmap = source.scale(scaledW, scaledH)
        canvas.drawBitmap(scaledBitmap, offsetX, offsetY, Paint(Paint.FILTER_BITMAP_FLAG))

        if (scaledBitmap !== source) scaledBitmap.recycle()

        return output
    }

    fun onNavigatedToResult() {
        _uiState.value = _uiState.value.copy(shouldNavigateToResult = false)
    }

    override fun onCleared() {
        super.onCleared()
        classifier.clear()
    }
}

