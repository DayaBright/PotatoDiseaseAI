package com.tesis.potatodiseaseai.data.tflite

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException

class ImageClassifier(context: Context) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var isInitialized = false

    private val inputSize = 224
    private val modelFile = "modelo_papa.tflite"
    private val labelsFile = "labels.txt"

    data class ClassifierResult(val label: String, val confidence: Float)

    init {
        try {
            Log.d(TAG, "Intentando cargar labels desde: $labelsFile")
            labels = FileUtil.loadLabels(context, labelsFile)
            Log.d(TAG, "✓ Labels cargados: ${labels.size} clases")
            labels.forEachIndexed { index, label ->
                Log.d(TAG, "  [$index] $label")
            }
        } catch (e: IOException) {
            Log.e(TAG, "✗ ERROR cargando labels: ${e.message}", e)
            labels = emptyList()
        }

        try {
            Log.d(TAG, "Intentando cargar modelo desde: $modelFile")
            val model = FileUtil.loadMappedFile(context, modelFile)
            Log.d(TAG, "✓ Modelo cargado (${model.capacity()} bytes)")

            interpreter = Interpreter(model)
            isInitialized = true
            Log.d(TAG, "✓ Modelo TFLite inicializado correctamente")
        } catch (e: IOException) {
            Log.e(TAG, "✗ ERROR cargando modelo: ${e.message}", e)
            Log.e(TAG, "   Verifica que exista: assets/$modelFile")
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "✗ ERROR inesperado: ${e.message}", e)
            isInitialized = false
        }
    }

    fun classify(bitmap: Bitmap): ClassifierResult {
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "⚠ Clasificador NO inicializado")
            return ClassifierResult("Modelo no disponible", 0f)
        }
        if (labels.isEmpty()) {
            Log.w(TAG, "⚠ No hay labels disponibles")
            return ClassifierResult("Sin labels", 0f)
        }

        return try {
            val inputTensor = interpreter!!.getInputTensor(0)
            val inputType = inputTensor.dataType()
            val inputShape = inputTensor.shape()
            Log.d(TAG, "Input tensor -> type: $inputType, shape: ${inputShape.contentToString()}")

            val (tensorType, processor) = if (inputType == DataType.FLOAT32) {
                DataType.FLOAT32 to ImageProcessor.Builder()
                    .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                    .add(NormalizeOp(127.5f, 127.5f))
                    .build()
            } else {
                DataType.UINT8 to ImageProcessor.Builder()
                    .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                    .build()
            }

            val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else bitmap

            val image = TensorImage(tensorType)
            image.load(argbBitmap)
            val processed = processor.process(image)

            val output = TensorBuffer.createFixedSize(
                intArrayOf(1, labels.size),
                DataType.FLOAT32
            )

            interpreter?.run(processed.buffer, output.buffer.rewind())

            val scores = output.floatArray
            Log.d("DEBUG_PAPA", "Scores crudos: ${scores.joinToString(", ")}")

            var maxIdx = 0
            var maxScore = -Float.MAX_VALUE
            for (i in scores.indices) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i]
                    maxIdx = i
                }
            }

            val softmaxScore = softmax(scores)[maxIdx]
            val label = if (maxIdx < labels.size) {
                labels[maxIdx]
                    .replace("Potato___", "")
                    .replace("_", " ")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            } else "Desconocido"

            Log.d(TAG, "✓ Predicción: $label (confianza: ${(softmaxScore * 100).toInt()}%)")
            ClassifierResult(label, softmaxScore)
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error durante clasificación: ${e.message}", e)
            ClassifierResult("Error", 0f)
        }
    }

    private fun softmax(scores: FloatArray): FloatArray {
        val expScores = FloatArray(scores.size)
        val maxScore = scores.maxOrNull() ?: 0f
        var sum = 0f

        for (i in scores.indices) {
            expScores[i] = kotlin.math.exp(scores[i] - maxScore)
            sum += expScores[i]
        }

        if (sum != 0f) {
            for (i in expScores.indices) {
                expScores[i] /= sum
            }
        }
        return expScores
    }

    fun close() {
        try {
            interpreter?.close()
            Log.d(TAG, "✓ Clasificador cerrado")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error cerrando: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "ImageClassifier"
    }
}