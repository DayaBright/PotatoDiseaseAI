package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test
import com.tesis.potatodiseaseai.utils.LabelNormalizer

/**
 * ==========================================================================
 *  PT-U05 — Normalización de etiquetas (LabelNormalizer)
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *  RF cubiertos: RF-06 (clasificación CNN), RNF-05 (normalización de etiquetas)
 *  HU cubierta: HU-03 (nombre de enfermedad legible)
 *  Verifica que LabelNormalizer.normalize() transforma correctamente
 *  las etiquetas crudas del modelo CNN al formato esperado por la BD.
 */
class LabelNormalizerUnitTest {

    // ── PT-U05a — Normalización de etiquetas con prefijo Potato___ ──

    @Test
    fun `PT-U05a — etiqueta con prefijo Potato___ se normaliza correctamente`() {
        val result = LabelNormalizer.normalize("Potato___Early_Blight")
        assertEquals(
            "Potato___Early_Blight debe normalizarse a 'early blight'",
            "early blight", result
        )
    }

    @Test
    fun `PT-U05b — etiqueta sin prefijo se normaliza correctamente`() {
        val result = LabelNormalizer.normalize("late_blight")
        assertEquals(
            "late_blight debe normalizarse a 'late blight'",
            "late blight", result
        )
    }

    @Test
    fun `PT-U05c — etiqueta healthy permanece igual`() {
        val result = LabelNormalizer.normalize("healthy")
        assertEquals("healthy", result)
    }

    @Test
    fun `PT-U05d — etiqueta con mayusculas mixtas se convierte a minusculas`() {
        val result = LabelNormalizer.normalize("Leafroll_Virus")
        assertEquals("leafroll virus", result)
    }

    @Test
    fun `PT-U05e — etiqueta con espacios extra se recorta`() {
        val result = LabelNormalizer.normalize("  mosaic_virus  ")
        assertEquals("mosaic virus", result)
    }

    @Test
    fun `PT-U05f — etiqueta nematode sin guiones bajos permanece igual`() {
        val result = LabelNormalizer.normalize("nematode")
        assertEquals("nematode", result)
    }

    @Test
    fun `PT-U05g — etiqueta pest sin guiones bajos permanece igual`() {
        val result = LabelNormalizer.normalize("pest")
        assertEquals("pest", result)
    }

    // PT-U05h — Todas las 8 clases se normalizan al formato esperado

    @Test
    fun `PT-U05h — las 8 clases del modelo se normalizan al formato esperado`() {
        val rawLabels = listOf(
            "early_blight", "healthy", "late_blight",
            "leafroll_virus", "mosaic_virus", "nematode", "pest", "z_no_potato"
        )
        val expectedNormalized = listOf(
            "early blight", "healthy", "late blight",
            "leafroll virus", "mosaic virus", "nematode", "pest", "z no potato"
        )

        for (i in rawLabels.indices) {
            assertEquals(
                "La etiqueta '${rawLabels[i]}' no se normaliza correctamente",
                expectedNormalized[i],
                LabelNormalizer.normalize(rawLabels[i])
            )
        }
    }

    @Test
    fun `PT-U05i — etiqueta vacia devuelve cadena vacia`() {
        val result = LabelNormalizer.normalize("")
        assertEquals("", result)
    }
}
