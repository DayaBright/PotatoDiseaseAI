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

class ImageClassifierHelper(context: Context) {

    private var classifier: ImageClassifier? = null
    private var initError: AppError? = null
    
    private val modelName = "modelo_papa.tflite"
    private val labels: List<String> = runCatching {
        context.assets.open("labels.txt").bufferedReader().use { it.readLines() }
    }.getOrDefault(emptyList())

    data class ClassifierResult(
        val label: String,
        val confidence: Float,
        val error: AppError? = null  // ✅ NUEVO: Incluir error en resultado
    )

    init {
        try {
            // Validar que el contexto sea applicationContext
            val appContext = context.applicationContext
            require(appContext === context || context === appContext) {
                "Debe pasar applicationContext para evitar memory leaks"
            }
            
            val options = ImageClassifierOptions.builder()
                .setMaxResults(1)
                .setScoreThreshold(0.0f)
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
        }
    }

    /**
     * Clasifica una imagen usando TensorFlow Lite
     * @param bitmap Imagen a clasificar
     * @return ClassifierResult con label, confidence y posible error
     */
    fun classify(bitmap: Bitmap): ClassifierResult {
        
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
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val results = localClassifier.classify(tensorImage)

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

            AppLogger.debug(TAG, "✓ Clasificación exitosa: $chosen (${category.score})")
            
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
     * Libera recursos del clasificador
     */
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
    
    /**
     * Verifica si el clasificador está listo para usar
     */
    fun isReady(): Boolean = classifier != null && initError == null

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}
