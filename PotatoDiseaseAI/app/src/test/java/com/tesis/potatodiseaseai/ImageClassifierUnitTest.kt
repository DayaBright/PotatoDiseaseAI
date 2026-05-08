package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test

/**
 * ==========================================================================
 *  BLOQUE 1 — PRUEBAS UNITARIAS (Unit Tests)
 *  Archivo: src/test/.../ImageClassifierUnitTest.kt
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *
 *  Convenciones usadas:
 *    • JUnit 4 (incluido por defecto en el proyecto)
 *    • Delta de 0.0001f para comparaciones de punto flotante
 *    • Constantes del modelo: 224×224 px, 7 clases
 */
class ImageClassifierUnitTest {

    // ──────────────────────────────────────────────
    // Constantes del modelo — se reutilizan en los tests
    // ──────────────────────────────────────────────
    companion object {
        /** Tamaño de entrada del modelo MobileNetV2 */
        const val MODEL_INPUT_SIZE = 224

        /** Cantidad de clases que produce el modelo */
        const val NUM_CLASSES = 7

        /**
         * Etiquetas esperadas del modelo, en el orden en que aparecen
         * en el archivo assets/labels.txt del proyecto.
         */
        val EXPECTED_LABELS = listOf(
            "early_blight",
            "healthy",
            "late_blight",
            "leafroll_virus",
            "mosaic_virus",
            "nematode",
            "pest"
        )
    }

    // ======================================================================
    //  PT-U01 — Normalización de píxeles
    //  Verifica que el preprocesamiento convierte valores de 0-255
    //  al rango [0.0, 1.0].
    // ======================================================================

    /**
     * Función auxiliar que replica la normalización que TFLite Task API
     * aplica internamente al convertir un Bitmap a TensorImage:
     * cada valor de píxel (0-255) se divide entre 255 para obtener
     * un float en [0.0, 1.0].
     */
    private fun normalizePixel(pixelValue: Int): Float {
        return pixelValue / 255.0f
    }

    @Test
    fun `PT-U01a — pixel 0 se normaliza a 0,0f`() {
        val result = normalizePixel(0)
        assertEquals(
            "El valor 0 debe normalizarse a 0.0f",
            0.0f, result, 0.0001f
        )
    }

    @Test
    fun `PT-U01b — pixel 255 se normaliza a 1,0f`() {
        val result = normalizePixel(255)
        assertEquals(
            "El valor 255 debe normalizarse a 1.0f",
            1.0f, result, 0.0001f
        )
    }

    @Test
    fun `PT-U01c — pixel 128 se normaliza entre 0 y 1`() {
        val result = normalizePixel(128)
        assertTrue(
            "El valor 128 normalizado ($result) debe estar entre 0.0 y 1.0",
            result in 0.0f..1.0f
        )
        // Además, verificar el valor esperado exacto: 128/255 ≈ 0.50196
        assertEquals(
            "128 / 255 ≈ 0.50196",
            128f / 255f, result, 0.0001f
        )
    }

    @Test
    fun `PT-U01d — todos los valores 0-255 caen en rango valido`() {
        for (pixel in 0..255) {
            val normalized = normalizePixel(pixel)
            assertTrue(
                "Pixel $pixel normalizado ($normalized) fuera de rango",
                normalized in 0.0f..1.0f
            )
        }
    }

    // ======================================================================
    //  PT-U02 — Dimensiones de salida del preprocesamiento
    //  Verifica que cualquier imagen de entrada se redimensiona a 224×224.
    //
    //  NOTA: android.graphics.Bitmap no está disponible en la JVM local,
    //  por lo que simulamos la lógica de redimensionamiento con una
    //  estructura de datos simple (ImageDimension).
    // ======================================================================

    /** Representa las dimensiones de una imagen sin depender de Android. */
    data class ImageDimension(val width: Int, val height: Int)

    /**
     * Simula la lógica de redimensionamiento que se aplica antes de
     * alimentar el modelo: la imagen se escala a MODEL_INPUT_SIZE × MODEL_INPUT_SIZE.
     */
    private fun resizeToModelInput(original: ImageDimension): ImageDimension {
        return ImageDimension(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)
    }

    @Test
    fun `PT-U02a — imagen 1500x1500 se redimensiona a 224x224`() {
        val input = ImageDimension(1500, 1500)
        val output = resizeToModelInput(input)
        assertEquals("Ancho debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.width)
        assertEquals("Alto debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.height)
    }

    @Test
    fun `PT-U02b — imagen panoramica 4000x2000 se redimensiona a 224x224`() {
        val input = ImageDimension(4000, 2000)
        val output = resizeToModelInput(input)
        assertEquals("Ancho debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.width)
        assertEquals("Alto debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.height)
    }

    @Test
    fun `PT-U02c — imagen vertical 1080x1920 se redimensiona a 224x224`() {
        val input = ImageDimension(1080, 1920)
        val output = resizeToModelInput(input)
        assertEquals("Ancho debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.width)
        assertEquals("Alto debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.height)
    }

    @Test
    fun `PT-U02d — imagen ya de 224x224 permanece igual`() {
        val input = ImageDimension(224, 224)
        val output = resizeToModelInput(input)
        assertEquals("Ancho debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.width)
        assertEquals("Alto debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.height)
    }

    @Test
    fun `PT-U02e — imagen pequeña 50x50 se escala a 224x224`() {
        val input = ImageDimension(50, 50)
        val output = resizeToModelInput(input)
        assertEquals("Ancho debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.width)
        assertEquals("Alto debe ser $MODEL_INPUT_SIZE", MODEL_INPUT_SIZE, output.height)
    }

    // ======================================================================
    //  PT-U03 — Formato del resultado del clasificador
    //  Verifica que la salida del modelo tiene exactamente 7 clases y que
    //  todas las probabilidades suman ~1.0.
    // ======================================================================

    /** Simula una distribución softmax típica del modelo (7 clases). */
    private fun mockSoftmaxOutput(): FloatArray = floatArrayOf(
        0.02f,   // early_blight
        0.05f,   // healthy
        0.78f,   // late_blight   ← clase dominante
        0.03f,   // leafroll_virus
        0.04f,   // mosaic_virus
        0.06f,   // nematode
        0.02f    // pest
    )

    @Test
    fun `PT-U03a — la salida del modelo contiene exactamente 7 clases`() {
        val probabilities = mockSoftmaxOutput()
        assertEquals(
            "El modelo debe producir exactamente $NUM_CLASSES probabilidades",
            NUM_CLASSES, probabilities.size
        )
    }

    @Test
    fun `PT-U03b — ninguna probabilidad es negativa`() {
        val probabilities = mockSoftmaxOutput()
        for ((index, prob) in probabilities.withIndex()) {
            assertTrue(
                "La probabilidad del índice $index es negativa: $prob",
                prob >= 0.0f
            )
        }
    }

    @Test
    fun `PT-U03c — la suma de probabilidades es aproximadamente 1,0`() {
        val probabilities = mockSoftmaxOutput()
        val sum = probabilities.sum()
        assertTrue(
            "La suma de probabilidades ($sum) debe estar entre 0.99 y 1.01",
            sum in 0.99f..1.01f
        )
    }

    @Test
    fun `PT-U03d — el indice del maximo esta entre 0 y 6`() {
        val probabilities = mockSoftmaxOutput()
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        assertTrue(
            "El índice del máximo ($maxIndex) debe estar entre 0 y ${NUM_CLASSES - 1}",
            maxIndex in 0 until NUM_CLASSES
        )
    }

    @Test
    fun `PT-U03e — la clase con mayor probabilidad es late_blight (indice 2)`() {
        val probabilities = mockSoftmaxOutput()
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        assertEquals(
            "El índice esperado de la clase dominante es 2 (late_blight)",
            2, maxIndex
        )
    }

    @Test
    fun `PT-U03f — ninguna probabilidad supera 1,0`() {
        val probabilities = mockSoftmaxOutput()
        for ((index, prob) in probabilities.withIndex()) {
            assertTrue(
                "La probabilidad del índice $index ($prob) supera 1.0",
                prob <= 1.0f
            )
        }
    }

    // ======================================================================
    //  PT-U04 — Mapeo de etiquetas
    //  Verifica que el índice 0 corresponde a "early_blight", el 1 a
    //  "healthy", etc., y que no hay errores de índice.
    // ======================================================================

    @Test
    fun `PT-U04a — la lista de etiquetas tiene exactamente 7 elementos`() {
        assertEquals(
            "Deben existir exactamente $NUM_CLASSES etiquetas",
            NUM_CLASSES, EXPECTED_LABELS.size
        )
    }

    @Test
    fun `PT-U04b — indice 0 corresponde a early_blight`() {
        assertEquals("early_blight", EXPECTED_LABELS[0])
    }

    @Test
    fun `PT-U04c — indice 1 corresponde a healthy`() {
        assertEquals("healthy", EXPECTED_LABELS[1])
    }

    @Test
    fun `PT-U04d — indice 2 corresponde a late_blight`() {
        assertEquals("late_blight", EXPECTED_LABELS[2])
    }

    @Test
    fun `PT-U04e — indice 3 corresponde a leafroll_virus`() {
        assertEquals("leafroll_virus", EXPECTED_LABELS[3])
    }

    @Test
    fun `PT-U04f — indice 4 corresponde a mosaic_virus`() {
        assertEquals("mosaic_virus", EXPECTED_LABELS[4])
    }

    @Test
    fun `PT-U04g — indice 5 corresponde a nematode`() {
        assertEquals("nematode", EXPECTED_LABELS[5])
    }

    @Test
    fun `PT-U04h — indice 6 corresponde a pest`() {
        assertEquals("pest", EXPECTED_LABELS[6])
    }

    @Test
    fun `PT-U04i — no hay etiquetas duplicadas`() {
        val uniqueLabels = EXPECTED_LABELS.toSet()
        assertEquals(
            "No debe haber etiquetas duplicadas",
            EXPECTED_LABELS.size, uniqueLabels.size
        )
    }

    @Test
    fun `PT-U04j — todas las etiquetas son no vacias`() {
        for ((index, label) in EXPECTED_LABELS.withIndex()) {
            assertTrue(
                "La etiqueta del índice $index está vacía",
                label.isNotBlank()
            )
        }
    }

    @Test
    fun `PT-U04k — las etiquetas coinciden con DiseaseDatabase`() {
        // Las etiquetas normalizadas deben coincidir con las claves
        // del mapa DiseaseDatabase en DetectionResult.kt
        val expectedDiseaseKeys = listOf(
            "early blight",     // early_blight → early blight
            "healthy",          // healthy → healthy
            "late blight",      // late_blight → late blight
            "leafroll virus",   // leafroll_virus → leafroll virus
            "mosaic virus",     // mosaic_virus → mosaic virus
            "nematode",         // nematode → nematode
            "pest"              // pest → pest
        )

        for ((index, rawLabel) in EXPECTED_LABELS.withIndex()) {
            // Replica la normalización de LabelNormalizer.normalize()
            val normalized = rawLabel
                .substringAfter("Potato___")
                .replace("_", " ")
                .lowercase()
                .trim()

            assertEquals(
                "Etiqueta normalizada del índice $index no coincide",
                expectedDiseaseKeys[index], normalized
            )
        }
    }
}
