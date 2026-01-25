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

    data class ClassifierResult(
        val label: String,
        val confidence: Float
    )

    init {
        try {
            val options = ImageClassifierOptions.builder()
                .setMaxResults(1)
                .setScoreThreshold(0.0f)
                .build()

            classifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                options
            )

            Log.d(TAG, "✓ Modelo cargado correctamente")
        } catch (e: IOException) {
            Log.e(TAG, "✗ Error cargando modelo: ${e.message}", e)
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
                val label = category.label
                    .replace("Potato___", "")
                    .replace("_", " ")

                ClassifierResult(label, category.score)
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

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}
