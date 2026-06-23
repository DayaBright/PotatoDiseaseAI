package com.tesis.potatodiseaseai.data.tflite

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.classifier.ImageClassifier.ImageClassifierOptions
import org.tensorflow.lite.support.image.TensorImage
import java.io.IOException
import com.tesis.potatodiseaseai.utils.LabelNormalizer        
import com.tesis.potatodiseaseai.utils.ErrorHandler          
import com.tesis.potatodiseaseai.utils.AppError              
import com.tesis.potatodiseaseai.utils.AppLogger
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix

class ImageClassifierHelper(context: Context) {

    private var classifier: ImageClassifier? = null
    private var initError: AppError? = null
    
    private val modelName = "potato_classifier.tflite"
    private val labels: List<String> = runCatching {
        context.assets.open("labels.txt").bufferedReader().use { it.readLines() }
    }.getOrDefault(emptyList())

    // Variables para el cálculo del tiempo promedio
    private var totalInferenceTimeMs = 0L
    private var inferenceCount = 0

    // Clase de datos para almacenar las métricas
    data class PerformanceMetrics(
        var loadTimeMs: Long = 0L,
        var averageInferenceTimeMs: Long = 0L,
        var memoryUsedMB: Double = 0.0
    )

    val metrics = PerformanceMetrics()

    data class ClassifierResult(
        val label: String,
        val confidence: Float,
        val error: AppError? = null  
    )

    init {
        val loadStartTime = System.nanoTime()
        try {
            val appContext = context.applicationContext

            val options = ImageClassifierOptions.builder()
                .setMaxResults(1)
                .setScoreThreshold(0.0f)
                .setBaseOptions(
                    org.tensorflow.lite.task.core.BaseOptions.builder()
                        .setNumThreads(4) // MobileNetV2 aprovecha 4 hilos
                        .build()
                )
                .build()

            classifier = ImageClassifier.createFromFileAndOptions(
                appContext,
                modelName,
                options
            )

            AppLogger.debug(TAG, "✓ Modelo cargado correctamente")
            
        } catch (e: IOException) {
            // ✅ Usar ErrorHandler centralizado
            initError = ErrorHandler.handleException(e, "Cargando modelo TensorFlow Lite")
            AppLogger.error(TAG, initError!!.message)
            
        } catch (e: IllegalArgumentException) {
            // ✅ Usar ErrorHandler centralizado
            initError = ErrorHandler.handleException(e, "Validación de contexto")
            AppLogger.error(TAG, initError!!.message)
            
        } catch (e: Exception) {
            // ✅ Usar ErrorHandler para excepciones genéricas
            initError = ErrorHandler.handleException(e, "Inicializando clasificador")
            AppLogger.error(TAG, initError!!.message)
        } finally {
            // 3. Obtener el tiempo de carga
            val loadEndTime = System.nanoTime()
            metrics.loadTimeMs = (loadEndTime - loadStartTime) / 1_000_000
            
            AppLogger.debug(TAG, "Métricas Iniciales -> " +
                    "Tiempo de Carga: ${metrics.loadTimeMs} ms")
        }
    }

    fun classify(bitmap: Bitmap, rotationDegrees: Int = 0): ClassifierResult {
        
        // ✅ Validar si hay error de inicialización
        if (initError != null) {
            return ClassifierResult(
                label = "",
                confidence = 0f,
                error = initError
            )
        }
        
        val localClassifier = classifier
        if (localClassifier == null) {
            val error = AppError.ClassificationError("Clasificador no disponible")
            AppLogger.error(TAG, error.message)
            return ClassifierResult(
                label = "",
                confidence = 0f,
                error = error
            )
        }

        // ✅ Validar bitmap
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            val error = AppError.ClassificationError("Imagen inválida (dimensiones incorrectas)")
            AppLogger.error(TAG, error.message)
            return ClassifierResult(
                label = "",
                confidence = 0f,
                error = error
            )
        }

        if (bitmap.byteCount == 0) {
            val error = AppError.ClassificationError("Imagen vacía")
            AppLogger.error(TAG, error.message)
            return ClassifierResult(
                label = "",
                confidence = 0f,
                error = error
            )
        }


        return try {
            // 1. Rotar primero si es necesario (corrige orientación del sensor de cámara)
            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else bitmap

            // 2. Letterbox: añade padding gris (114,114,114) igual que en el entrenamiento Python
            //    Esto preserva el aspecto real de la hoja sin recortar, a diferencia del
            //    ResizeWithCropOrPadOp que solo recortaba el centro geométrico del frame.
            val letterboxed = letterbox(rotatedBitmap, INPUT_SIZE)

            // 3. Inferencia directa con el bitmap ya preparado
            val tensorImage = TensorImage.fromBitmap(letterboxed)

            val startTime = System.nanoTime()
            val results = localClassifier.classify(tensorImage)
            val endTime = System.nanoTime()

            // Liberar bitmaps intermedios
            if (rotatedBitmap !== bitmap) rotatedBitmap.recycle()
            letterboxed.recycle()

            val inferenceTimeMs = (endTime - startTime) / 1_000_000

            // 4. Actualizar el tiempo de inferencia promedio
            inferenceCount++
            totalInferenceTimeMs += inferenceTimeMs
            metrics.averageInferenceTimeMs = totalInferenceTimeMs / inferenceCount

            // Mostrar métricas después de 10 inferencias
            if (inferenceCount == 10) {
                // 5. Obtener memoria RAM (PSS) utilizada por la app
                val memoryInfo = android.os.Debug.MemoryInfo()
                android.os.Debug.getMemoryInfo(memoryInfo)
                // totalPss devuelve el tamaño en KB, lo dividimos por 1024 para MB
                metrics.memoryUsedMB = memoryInfo.totalPss / 1024.0

                AppLogger.debug(TAG, """
                    ================ MÉTRICAS TESIS ================
                    Tiempo de Carga: ${metrics.loadTimeMs} ms
                    Tiempo Promedio (10 inferencias): ${metrics.averageInferenceTimeMs} ms
                    Memoria RAM Utilizada: %.2f MB
                    ================================================
                """.trimIndent().format(metrics.memoryUsedMB))
            }

            if (results.isEmpty() || results[0].categories.isEmpty()) {
                val error = AppError.ClassificationError("No se detectaron resultados")
                AppLogger.warning(TAG, error.message)
                return ClassifierResult(
                    label = "",
                    confidence = 0f,
                    error = error
                )
            }

            val category = results[0].categories[0]

            val fromDisplay = category.displayName.takeIf { it.isNotBlank() }
            val fromLabels = labels.getOrNull(category.index)
            val raw = category.label

            // Usar normalización centralizada
            val chosen = LabelNormalizer.normalize(
                fromDisplay ?: fromLabels ?: raw
            )
            
            ClassifierResult(
                label = chosen,
                confidence = category.score,
                error = null
            )
            
        } catch (e: IllegalArgumentException) {
            // Error de argumento inválido
            val error = ErrorHandler.handleException(e, "Argumento de clasificación")
            AppLogger.error(TAG, error.message, e)
            ClassifierResult(
                label = "",
                confidence = 0f,
                error = error
            )
            
        } catch (e: IllegalStateException) {
            // Error de estado
            val error = AppError.ClassificationError("Estado inválido del clasificador: ${e.message}")
            AppLogger.error(TAG, error.message, e)
            ClassifierResult(
                label = "",
                confidence = 0f,
                error = error
            )
            
        } catch (e: IOException) {
            // Error de E/S
            val error = ErrorHandler.handleException(e, "Clasificación (E/S)")
            AppLogger.error(TAG, error.message, e)
            ClassifierResult(
                label = "",
                confidence = 0f,
                error = error
            )
            
        } catch (e: Exception) {
            // Error genérico
            val error = ErrorHandler.handleException(e, "Clasificación")
            AppLogger.error(TAG, error.message, e)
            ClassifierResult(
                label = "",
                confidence = 0f,
                error = error
            )
        }
    }

    /**
     * Letterbox: equivalente exacto de letterbox_pil() del notebook Python.
     * Rellena con gris medio (114, 114, 114) = MEAN_COLOR usado en el entrenamiento.
     * Preserva el aspecto de la imagen original sin recortar contenido.
     */
    private fun letterbox(src: Bitmap, targetSize: Int): Bitmap {
        val w = src.width
        val h = src.height
        val side = maxOf(w, h)

        // Canvas cuadrado con color de relleno igual al del entrenamiento
        val squared = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(squared)
        canvas.drawColor(Color.rgb(114, 114, 114))

        val left = (side - w) / 2
        val top = (side - h) / 2
        canvas.drawBitmap(src, left.toFloat(), top.toFloat(), null)

        // Redimensionar a targetSize × targetSize
        val result = Bitmap.createScaledBitmap(squared, targetSize, targetSize, true)
        squared.recycle()
        return result
    }

    fun clear() {
        try {
            classifier?.close()
            classifier = null
            AppLogger.debug(TAG, "✓ Clasificador liberado correctamente")
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, "Liberando clasificador")
            AppLogger.error(TAG, error.message)
        }
    }

    fun isReady(): Boolean = classifier != null && initError == null

    companion object {
        private const val TAG = "ImageClassifierHelper"
        private const val INPUT_SIZE = 224 // Tamaño de entrada del modelo MobileNetV2
    }
}
