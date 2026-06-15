package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test
import com.tesis.potatodiseaseai.utils.ImageUtils

/**
 * ==========================================================================
 *  PT-U12 — Transformación de letterbox (Padding para el modelo)
 *  Archivo: src/test/.../LetterboxUnitTest.kt
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *
 *  RF cubierto: RNF-04 (precisión del modelo, preprocesamiento)
 *  HU cubierta: N/A (requisito técnico)
 *
 *  Verifica la lógica matemática pura del escalado letterbox para que
 *  las imágenes quepan dentro de un cuadrado sin perder la proporción original.
 */
class LetterboxUnitTest {

    // ── PT-U12a — Escala proporcional de imagen apaisada (landscape) ──

    @Test
    fun `PT-U12a — imagen apaisada se escala respetando proporcion`() {
        // Imagen 640x480, target 224
        // scale = 224 / 640 = 0.35
        // scaledW = 224, scaledH = 480 * 0.35 = 168
        val result = ImageUtils.calculateLetterbox(640f, 480f, 224)

        assertEquals("El lado mayor debe igualar al targetSize", 224, result.scaledW)
        assertEquals("El lado menor debe escalar proporcionalmente", 168, result.scaledH)
    }

    // ── PT-U12b — Escala proporcional de imagen vertical (portrait) ──

    @Test
    fun `PT-U12b — imagen vertical se escala respetando proporcion`() {
        // Imagen 1080x1920, target 224
        // scale = 224 / 1920 = 0.11666...
        // scaledW = 1080 * 0.11666... = 126
        // scaledH = 224
        val result = ImageUtils.calculateLetterbox(1080f, 1920f, 224)

        assertEquals("El lado menor debe escalar proporcionalmente", 126, result.scaledW)
        assertEquals("El lado mayor debe igualar al targetSize", 224, result.scaledH)
    }

    // ── PT-U12c — Escala de imagen ya cuadrada ──

    @Test
    fun `PT-U12c — imagen cuadrada llena todo el targetSize`() {
        // Imagen 500x500, target 224
        val result = ImageUtils.calculateLetterbox(500f, 500f, 224)

        assertEquals(224, result.scaledW)
        assertEquals(224, result.scaledH)
        assertEquals("No debe haber padding X", 0f, result.offsetX)
        assertEquals("No debe haber padding Y", 0f, result.offsetY)
    }

    // ── PT-U12d — Cálculo de padding (offsets) ──

    @Test
    fun `PT-U12d — el padding debe centrar la imagen en el canvas`() {
        // Apaisada: 640x480 -> 224x168
        // Padding total Y = 224 - 168 = 56
        // offsetY debe ser 56 / 2 = 28
        val result = ImageUtils.calculateLetterbox(640f, 480f, 224)

        assertEquals("No debe haber padding X para apaisadas", 0f, result.offsetX)
        assertEquals("Padding Y debe ser la mitad del espacio restante", 28f, result.offsetY)
    }

    @Test
    fun `PT-U12e — el padding debe centrar imagen vertical en el canvas`() {
        // Vertical: 480x640 -> 168x224
        // Padding total X = 224 - 168 = 56
        // offsetX debe ser 56 / 2 = 28
        val result = ImageUtils.calculateLetterbox(480f, 640f, 224)

        assertEquals("Padding X debe ser la mitad del espacio restante", 28f, result.offsetX)
        assertEquals("No debe haber padding Y para verticales", 0f, result.offsetY)
    }
}
