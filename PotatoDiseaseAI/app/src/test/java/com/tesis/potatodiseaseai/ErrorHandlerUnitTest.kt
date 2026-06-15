package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test
import com.tesis.potatodiseaseai.utils.AppError
import com.tesis.potatodiseaseai.utils.ErrorHandler

/**
 * ==========================================================================
 *  PT-U09 — Manejo de errores centralizado (ErrorHandler / AppError)
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *  RNF cubierto: RNF-03 (jerarquía de errores y mensajes amigables)
 *  Verifica que la jerarquía sealed class AppError clasifica correctamente
 *  los errores y que los mensajes por defecto son informativos utilizando
 *  la implementación real del sistema.
 */
class ErrorHandlerUnitTest {

    // ── PT-U09d — Mensajes por defecto no están vacíos ──

    @Test
    fun `PT-U09d — todos los errores tienen mensaje por defecto no vacio`() {
        val errores = listOf(
            AppError.ImageSaveError(),
            AppError.DatabaseError(),
            AppError.ClassificationError(),
            AppError.CameraError(),
            AppError.FileDeleteError(),
            AppError.UnknownError()
        )

        for (error in errores) {
            assertTrue(
                "El mensaje de ${error::class.simpleName} no debe estar vacío",
                error.message.isNotBlank()
            )
        }
    }

    // ── PT-U09e — Mensajes amigables para el usuario ──

    @Test
    fun `PT-U09e — ImageSaveError tiene mensaje amigable`() {
        val msg = ErrorHandler.getUserMessage(AppError.ImageSaveError())
        assertTrue("Debe contener indicación de reintento", msg.contains("Intenta"))
    }

    @Test
    fun `PT-U09f — DatabaseError tiene mensaje amigable`() {
        val msg = ErrorHandler.getUserMessage(AppError.DatabaseError())
        assertTrue("Debe mencionar el historial", msg.contains("historial"))
    }

    @Test
    fun `PT-U09g — ClassificationError tiene mensaje amigable`() {
        val msg = ErrorHandler.getUserMessage(AppError.ClassificationError())
        assertTrue("Debe sugerir otra foto", msg.contains("otra foto"))
    }

    @Test
    fun `PT-U09h — CameraError tiene mensaje amigable`() {
        val msg = ErrorHandler.getUserMessage(AppError.CameraError())
        assertTrue("Debe mencionar permisos", msg.contains("permisos"))
    }

    @Test
    fun `PT-U09i — FileDeleteError tiene mensaje amigable`() {
        val msg = ErrorHandler.getUserMessage(AppError.FileDeleteError())
        assertTrue("Debe mencionar eliminación", msg.contains("eliminar"))
    }

    @Test
    fun `PT-U09j — UnknownError tiene mensaje amigable`() {
        val msg = ErrorHandler.getUserMessage(AppError.UnknownError())
        assertTrue("Debe indicar error inesperado", msg.contains("inesperado"))
    }

    // ── PT-U09k — Mensajes personalizados se preservan ──

    @Test
    fun `PT-U09k — mensaje personalizado se preserva en el error`() {
        val customMsg = "Fallo en la conexión con Room"
        val error = AppError.DatabaseError(customMsg)
        assertEquals(
            "El mensaje personalizado debe preservarse",
            customMsg, error.message
        )
    }
}
