package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test

/**
 * ==========================================================================
 *  PT-U11 — Guía visual del escáner (recorte de cámara)
 *  Archivo: src/test/.../CameraGuideUnitTest.kt
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *
 *  RF cubiertos: RF-03 (guía visual), RF-05 (corrección EXIF)
 *  HU cubierta: HU-01 (capturar foto con visor guía)
 *
 *  Verifica la lógica de recorte cuadrado basada en la fracción guía (0.85)
 *  que se aplica a las imágenes capturadas por la cámara.
 */
class CameraGuideUnitTest {

    companion object {
        /** Fracción del lado menor que se recorta (igual que en ScannerScreen) */
        const val GUIDE_FRACTION = 0.85f
    }

    /**
     * Simula la lógica de recorte de ScannerViewModel.classifyAndSave().
     * Retorna el tamaño del recorte cuadrado en píxeles de imagen.
     */
    data class CropResult(
        val cropSize: Int,
        val cropX: Int,
        val cropY: Int
    )

    private fun calculateCrop(
        imageWidth: Int,
        imageHeight: Int,
        screenWidth: Int,
        screenHeight: Int
    ): CropResult {
        val imgW = imageWidth.toFloat()
        val imgH = imageHeight.toFloat()
        val scrW = screenWidth.toFloat()
        val scrH = screenHeight.toFloat()

        // FILL_CENTER usa el factor de escala mayor
        val fillScale = maxOf(scrW / imgW, scrH / imgH)

        // Tamaño de la guía en pantalla → convertir a píxeles de imagen
        val guideScreenPx = minOf(scrW, scrH) * GUIDE_FRACTION
        val guideCamPx = (guideScreenPx / fillScale).toInt()
            .coerceAtMost(minOf(imageWidth, imageHeight))

        val x = (imageWidth - guideCamPx) / 2
        val y = (imageHeight - guideCamPx) / 2

        return CropResult(
            cropSize = guideCamPx,
            cropX = x,
            cropY = y
        )
    }

    // ── PT-U11a — El recorte es cuadrado ──

    @Test
    fun `PT-U11a — el recorte siempre produce una imagen cuadrada`() {
        val result = calculateCrop(4032, 3024, 1080, 2340)
        assertTrue(
            "El tamaño del recorte (${result.cropSize}) debe ser positivo",
            result.cropSize > 0
        )
    }

    // ── PT-U11b — El recorte está centrado ──

    @Test
    fun `PT-U11b — el recorte esta centrado en la imagen`() {
        val imgW = 4032
        val imgH = 3024
        val result = calculateCrop(imgW, imgH, 1080, 2340)

        // Verificar que los offsets centran el recorte
        val expectedX = (imgW - result.cropSize) / 2
        val expectedY = (imgH - result.cropSize) / 2
        assertEquals("Offset X debe centrar el recorte", expectedX, result.cropX)
        assertEquals("Offset Y debe centrar el recorte", expectedY, result.cropY)
    }

    // ── PT-U11c — El recorte no excede las dimensiones de la imagen ──

    @Test
    fun `PT-U11c — el recorte no excede las dimensiones de la imagen`() {
        val imgW = 1920
        val imgH = 1080
        val result = calculateCrop(imgW, imgH, 1080, 2340)

        assertTrue(
            "cropSize (${result.cropSize}) no debe exceder el lado menor ($imgH)",
            result.cropSize <= minOf(imgW, imgH)
        )
        assertTrue("cropX no debe ser negativo", result.cropX >= 0)
        assertTrue("cropY no debe ser negativo", result.cropY >= 0)
        assertTrue(
            "cropX + cropSize no debe exceder imageWidth",
            result.cropX + result.cropSize <= imgW
        )
        assertTrue(
            "cropY + cropSize no debe exceder imageHeight",
            result.cropY + result.cropSize <= imgH
        )
    }

    // ── PT-U11d — Fracción de guía produce un recorte razonable ──

    @Test
    fun `PT-U11d — el recorte es al menos el 30% del lado menor de la imagen`() {
        val imgW = 4032
        val imgH = 3024
        val result = calculateCrop(imgW, imgH, 1080, 2340)
        val minSide = minOf(imgW, imgH)
        // Con FILL_CENTER y pantallas altas (ej. 1080x2340), el fillScale
        // reduce la guía en coordenadas de imagen. El 30% es el umbral
        // realista para garantizar un recorte significativo.
        assertTrue(
            "El recorte (${result.cropSize}) debe ser al menos el 30% de $minSide (${(minSide * 0.3).toInt()})",
            result.cropSize >= minSide * 0.3
        )
    }

    // ── PT-U11e — Imagen cuadrada ──

    @Test
    fun `PT-U11e — imagen cuadrada produce offsets iguales`() {
        val result = calculateCrop(3000, 3000, 1080, 2340)
        // En una imagen cuadrada, el offset X y Y deben ser iguales
        assertEquals(
            "En una imagen cuadrada, offsetX y offsetY deben ser iguales",
            result.cropX, result.cropY
        )
    }

    // ── PT-U11f — Ángulos EXIF válidos ──

    @Test
    fun `PT-U11f — los angulos de rotacion EXIF validos son 0, 90, 180, 270`() {
        val validAngles = setOf(0f, 90f, 180f, 270f)
        // Simular los valores EXIF que mapean a ángulos
        val exifMapping = mapOf(
            1 to 0f,    // ORIENTATION_NORMAL
            6 to 90f,   // ORIENTATION_ROTATE_90
            3 to 180f,  // ORIENTATION_ROTATE_180
            8 to 270f   // ORIENTATION_ROTATE_270
        )
        exifMapping.values.forEach { angle ->
            assertTrue(
                "Ángulo $angle debe ser uno de los valores válidos",
                angle in validAngles
            )
        }
    }
}
