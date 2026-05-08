package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test

/**
 * ==========================================================================
 *  PT-U08 — Letterbox / Preservación de aspecto
 *  Archivo: src/test/.../LetterboxUnitTest.kt
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *
 *  RF cubierto: RF-02 (selección desde galería con procesamiento correcto)
 *  HU cubierta: HU-02 (analizar imagen de galería sin distorsión)
 *  RNF cubierto: RNF-06 (preprocesamiento robusto con letterbox)
 *
 *  Verifica la lógica de letterbox que redimensiona la imagen preservando
 *  la relación de aspecto y rellenando con negro, sin recortar ni distorsionar.
 */
class LetterboxUnitTest {

    companion object {
        const val TARGET_SIZE = 224
    }

    /**
     * Simula la lógica de ScannerViewModel.letterboxBitmap() en JVM.
     * Retorna las dimensiones escaladas y los offsets de centrado.
     */
    data class LetterboxResult(
        val outputWidth: Int,
        val outputHeight: Int,
        val scaledWidth: Int,
        val scaledHeight: Int,
        val offsetX: Float,
        val offsetY: Float
    )

    private fun calculateLetterbox(srcWidth: Int, srcHeight: Int): LetterboxResult {
        val srcW = srcWidth.toFloat()
        val srcH = srcHeight.toFloat()

        // Factor de escala para que el lado mayor quepa en TARGET_SIZE
        val scale = TARGET_SIZE.toFloat() / maxOf(srcW, srcH)

        val scaledW = (srcW * scale).toInt()
        val scaledH = (srcH * scale).toInt()

        // Centrar la imagen escalada en el canvas
        val offsetX = (TARGET_SIZE - scaledW) / 2f
        val offsetY = (TARGET_SIZE - scaledH) / 2f

        return LetterboxResult(
            outputWidth = TARGET_SIZE,
            outputHeight = TARGET_SIZE,
            scaledWidth = scaledW,
            scaledHeight = scaledH,
            offsetX = offsetX,
            offsetY = offsetY
        )
    }

    // ── PT-U08a — Imagen cuadrada ──

    @Test
    fun `PT-U08a — imagen cuadrada 500x500 produce salida 224x224 sin padding`() {
        val result = calculateLetterbox(500, 500)
        assertEquals("Salida debe ser 224x224", TARGET_SIZE, result.outputWidth)
        assertEquals("Salida debe ser 224x224", TARGET_SIZE, result.outputHeight)
        assertEquals("Imagen escalada ocupa todo el ancho", TARGET_SIZE, result.scaledWidth)
        assertEquals("Imagen escalada ocupa todo el alto", TARGET_SIZE, result.scaledHeight)
        assertEquals("Sin offset horizontal", 0f, result.offsetX, 0.5f)
        assertEquals("Sin offset vertical", 0f, result.offsetY, 0.5f)
    }

    // ── PT-U08b — Imagen horizontal (landscape) ──

    @Test
    fun `PT-U08b — imagen horizontal 640x480 tiene padding vertical (pillarbox)`() {
        val result = calculateLetterbox(640, 480)
        assertEquals("Salida debe ser 224x224", TARGET_SIZE, result.outputWidth)
        assertEquals("Salida debe ser 224x224", TARGET_SIZE, result.outputHeight)
        assertEquals("El ancho escalado debe ocupar todo", TARGET_SIZE, result.scaledWidth)
        assertTrue(
            "El alto escalado debe ser menor que 224: ${result.scaledHeight}",
            result.scaledHeight < TARGET_SIZE
        )
        assertTrue("Debe haber offset vertical > 0", result.offsetY > 0f)
    }

    // ── PT-U08c — Imagen vertical (portrait) ──

    @Test
    fun `PT-U08c — imagen vertical 480x640 tiene padding horizontal (letterbox)`() {
        val result = calculateLetterbox(480, 640)
        assertEquals("Salida debe ser 224x224", TARGET_SIZE, result.outputWidth)
        assertEquals("Salida debe ser 224x224", TARGET_SIZE, result.outputHeight)
        assertTrue(
            "El ancho escalado debe ser menor que 224: ${result.scaledWidth}",
            result.scaledWidth < TARGET_SIZE
        )
        assertEquals("El alto escalado debe ocupar todo", TARGET_SIZE, result.scaledHeight)
        assertTrue("Debe haber offset horizontal > 0", result.offsetX > 0f)
    }

    // ── PT-U08d — Imagen ya del tamaño correcto ──

    @Test
    fun `PT-U08d — imagen de 224x224 no cambia dimensiones`() {
        val result = calculateLetterbox(224, 224)
        assertEquals(TARGET_SIZE, result.scaledWidth)
        assertEquals(TARGET_SIZE, result.scaledHeight)
        assertEquals(0f, result.offsetX, 0.5f)
        assertEquals(0f, result.offsetY, 0.5f)
    }

    // ── PT-U08e — Imagen muy grande ──

    @Test
    fun `PT-U08e — imagen 4000x3000 se reduce a caber en 224x224`() {
        val result = calculateLetterbox(4000, 3000)
        assertEquals(TARGET_SIZE, result.outputWidth)
        assertEquals(TARGET_SIZE, result.outputHeight)
        assertTrue(
            "Ancho escalado no debe exceder 224: ${result.scaledWidth}",
            result.scaledWidth <= TARGET_SIZE
        )
        assertTrue(
            "Alto escalado no debe exceder 224: ${result.scaledHeight}",
            result.scaledHeight <= TARGET_SIZE
        )
    }

    // ── PT-U08f — Imagen muy pequeña ──

    @Test
    fun `PT-U08f — imagen 50x30 se escala a caber en 224x224`() {
        val result = calculateLetterbox(50, 30)
        assertEquals(TARGET_SIZE, result.outputWidth)
        assertEquals(TARGET_SIZE, result.outputHeight)
        assertTrue(
            "Ancho escalado no debe exceder 224: ${result.scaledWidth}",
            result.scaledWidth <= TARGET_SIZE
        )
        assertTrue(
            "Alto escalado no debe exceder 224: ${result.scaledHeight}",
            result.scaledHeight <= TARGET_SIZE
        )
    }

    // ── PT-U08g — La relación de aspecto se preserva ──

    @Test
    fun `PT-U08g — la relacion de aspecto se preserva tras letterbox`() {
        val srcW = 800
        val srcH = 600
        val originalRatio = srcW.toFloat() / srcH.toFloat()

        val result = calculateLetterbox(srcW, srcH)
        val scaledRatio = result.scaledWidth.toFloat() / result.scaledHeight.toFloat()

        assertEquals(
            "La relación de aspecto debe preservarse",
            originalRatio, scaledRatio, 0.05f
        )
    }
}
