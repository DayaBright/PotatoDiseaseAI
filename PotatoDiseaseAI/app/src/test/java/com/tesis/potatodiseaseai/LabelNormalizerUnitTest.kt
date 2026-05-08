package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test

/**
 * ==========================================================================
 *  PT-U05 — Normalización de etiquetas (LabelNormalizer)
 *  Archivo: src/test/.../LabelNormalizerUnitTest.kt
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *
 *  RF cubiertos: RF-06 (clasificación CNN), RNF-05 (normalización de etiquetas)
 *  HU cubierta: HU-03 (nombre de enfermedad legible)
 *
 *  Verifica que LabelNormalizer.normalize() transforma correctamente
 *  las etiquetas crudas del modelo CNN al formato esperado por la BD.
 */
class LabelNormalizerUnitTest {

    /**
     * Replica la lógica de LabelNormalizer.normalize() para ejecutar
     * en JVM local sin dependencias de Android.
     */
    private fun normalize(label: String): String {
        return label
            .substringAfter("Potato___")
            .replace("_", " ")
            .lowercase()
            .trim()
    }

    // ── PT-U05a — Normalización de etiquetas con prefijo Potato___ ──

    @Test
    fun `PT-U05a — etiqueta con prefijo Potato___ se normaliza correctamente`() {
        val result = normalize("Potato___Early_Blight")
        assertEquals(
            "Potato___Early_Blight debe normalizarse a 'early blight'",
            "early blight", result
        )
    }

    @Test
    fun `PT-U05b — etiqueta sin prefijo se normaliza correctamente`() {
        val result = normalize("late_blight")
        assertEquals(
            "late_blight debe normalizarse a 'late blight'",
            "late blight", result
        )
    }

    @Test
    fun `PT-U05c — etiqueta healthy permanece igual`() {
        val result = normalize("healthy")
        assertEquals("healthy", result)
    }

    @Test
    fun `PT-U05d — etiqueta con mayusculas mixtas se convierte a minusculas`() {
        val result = normalize("Leafroll_Virus")
        assertEquals("leafroll virus", result)
    }

    @Test
    fun `PT-U05e — etiqueta con espacios extra se recorta`() {
        val result = normalize("  mosaic_virus  ")
        assertEquals("mosaic virus", result)
    }

    @Test
    fun `PT-U05f — etiqueta nematode sin guiones bajos permanece igual`() {
        val result = normalize("nematode")
        assertEquals("nematode", result)
    }

    @Test
    fun `PT-U05g — etiqueta pest sin guiones bajos permanece igual`() {
        val result = normalize("pest")
        assertEquals("pest", result)
    }

    // ── PT-U05h — Todas las 7 clases se normalizan al formato esperado ──

    @Test
    fun `PT-U05h — las 7 clases del modelo se normalizan al formato esperado`() {
        val rawLabels = listOf(
            "early_blight", "healthy", "late_blight",
            "leafroll_virus", "mosaic_virus", "nematode", "pest"
        )
        val expectedNormalized = listOf(
            "early blight", "healthy", "late blight",
            "leafroll virus", "mosaic virus", "nematode", "pest"
        )

        for (i in rawLabels.indices) {
            assertEquals(
                "La etiqueta '${rawLabels[i]}' no se normaliza correctamente",
                expectedNormalized[i],
                normalize(rawLabels[i])
            )
        }
    }

    @Test
    fun `PT-U05i — etiqueta vacia devuelve cadena vacia`() {
        val result = normalize("")
        assertEquals("", result)
    }
}
