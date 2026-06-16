package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test
import com.tesis.potatodiseaseai.utils.LabelNormalizer

/**
 * ==========================================================================
 *  BLOQUE 1 — PRUEBAS UNITARIAS (Unit Tests)
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 */
class ImageClassifierUnitTest {

    companion object {
        const val NUM_CLASSES = 8

        val EXPECTED_LABELS = listOf(
            "early_blight",
            "healthy",
            "late_blight",
            "leafroll_virus",
            "mosaic_virus",
            "nematode",
            "pest",
            "z_no_potato"
        )
    }

    // ======================================================================
    //  LabelNormalizer Tests
    //  Verifica que la lógica real del sistema normalice correctamente
    // ======================================================================

    @Test
    fun `LabelNormalizer — elimina el prefijo Potato___`() {
        val result = LabelNormalizer.normalize("Potato___early_blight")
        assertEquals("early blight", result)
    }

    @Test
    fun `LabelNormalizer — reemplaza guiones bajos por espacios`() {
        val result = LabelNormalizer.normalize("late_blight")
        assertEquals("late blight", result)
    }

    @Test
    fun `LabelNormalizer — convierte a minusculas`() {
        val result = LabelNormalizer.normalize("HEALTHY")
        assertEquals("healthy", result)
    }

    @Test
    fun `LabelNormalizer — procesa correctamente z_no_potato`() {
        val result = LabelNormalizer.normalize("z_no_potato")
        assertEquals("z no potato", result)
    }

    @Test
    fun `LabelNormalizer — aplica multiples transformaciones a la vez`() {
        val result = LabelNormalizer.normalize("Potato___Late_Blight ")
        assertEquals("late blight", result)
    }

    // ======================================================================
    //  Validación de Etiquetas Esperadas
    // ======================================================================

    @Test
    fun `Etiquetas — la lista tiene exactamente 8 clases`() {
        assertEquals(
            "Deben existir exactamente $NUM_CLASSES etiquetas",
            NUM_CLASSES, EXPECTED_LABELS.size
        )
    }

    @Test
    fun `Etiquetas — z_no_potato esta presente en las etiquetas`() {
        assertTrue(
            "z_no_potato debe ser parte de las etiquetas del modelo",
            EXPECTED_LABELS.contains("z_no_potato")
        )
    }

    @Test
    fun `Etiquetas — no hay etiquetas duplicadas`() {
        val uniqueLabels = EXPECTED_LABELS.toSet()
        assertEquals(
            "No debe haber etiquetas duplicadas",
            EXPECTED_LABELS.size, uniqueLabels.size
        )
    }

    @Test
    fun `Etiquetas — todas las etiquetas son no vacias`() {
        for ((index, label) in EXPECTED_LABELS.withIndex()) {
            assertTrue(
                "La etiqueta del índice $index está vacía",
                label.isNotBlank()
            )
        }
    }

    @Test
    fun `Etiquetas — las etiquetas coinciden con LabelNormalizer esperado`() {
        val expectedDiseaseKeys = listOf(
            "early blight",
            "healthy",
            "late blight",
            "leafroll virus",
            "mosaic virus",
            "nematode",
            "pest",
            "z no potato"
        )

        for ((index, rawLabel) in EXPECTED_LABELS.withIndex()) {
            val normalized = LabelNormalizer.normalize(rawLabel)
            assertEquals(
                "Etiqueta normalizada del índice $index no coincide",
                expectedDiseaseKeys[index], normalized
            )
        }
    }
}
