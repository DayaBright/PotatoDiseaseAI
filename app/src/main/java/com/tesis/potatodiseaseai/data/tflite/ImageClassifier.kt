package com.tesis.potatodiseaseai.data.tflite

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.classifier.ImageClassifier.ImageClassifierOptions
import org.tensorflow.lite.support.image.TensorImage
import java.io.IOException

class ImageClassifierHelper(context: Context) {

    private var classifier: ImageClassifier? = null
    private val modelName = "modelo_papa.tflite"
    private val labels: List<String> = runCatching {
        context.assets.open("labels.txt").bufferedReader().use { it.readLines() }
    }.getOrDefault(emptyList())

    data class ClassifierResult(
        val label: String,
        val confidence: Float
    )

    init {
        try {
            // ✅ Validar que el contexto sea applicationContext
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

            Log.d(TAG, "✓ Modelo cargado correctamente")
        } catch (e: IOException) {
            Log.e(TAG, "✗ Error cargando modelo: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "✗ Error de contexto: ${e.message}", e)
        }
    }

    fun classify(bitmap: Bitmap): ClassifierResult {
        val localClassifier = classifier
            ?: return ClassifierResult("Modelo no disponible", 0f)

        return try {
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val results = localClassifier.classify(tensorImage)

            if (results.isNotEmpty() && results[0].categories.isNotEmpty()) {
                val category = results[0].categories[0]

                // 1) displayName si viene en el modelo
                val fromDisplay = category.displayName.takeIf { it.isNotBlank() }

                // 2) labels.txt por índice
                val fromLabels = labels.getOrNull(category.index)

                // 3) label crudo
                val raw = category.label

                val chosen = (fromDisplay ?: fromLabels ?: raw)
                    .substringAfter("Potato___")
                    .replace("_", " ")
                    .lowercase()
                    .trim()

                ClassifierResult(chosen, category.score)
            } else {
                ClassifierResult("No detectado", 0f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error clasificando: ${e.message}", e)
            ClassifierResult("Error", 0f)
        }
    }

    fun clear() {
        classifier?.close()
        classifier = null
        Log.d(TAG, "✓ Clasificador liberado")
    }
    
    // ✅ NUEVO: Verificar si el clasificador está listo
    fun isReady(): Boolean = classifier != null

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}
