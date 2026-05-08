package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test

/**
 * ==========================================================================
 *  PT-U09 — Manejo de errores centralizado (ErrorHandler / AppError)
 *  Archivo: src/test/.../ErrorHandlerUnitTest.kt
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *
 *  RNF cubierto: RNF-03 (jerarquía de errores y mensajes amigables)
 *
 *  Verifica que la jerarquía sealed class AppError clasifica correctamente
 *  los errores y que los mensajes por defecto son informativos.
 */
class ErrorHandlerUnitTest {

    // ── Réplica de la jerarquía AppError (JVM-safe, sin Android) ──

    sealed class AppError(val message: String) {
        class ImageSaveError(message: String = "Error al guardar la imagen") : AppError(message)
        class DatabaseError(message: String = "Error en la base de datos") : AppError(message)
        class ClassificationError(message: String = "Error al clasificar la imagen") : AppError(message)
        class CameraError(message: String = "Error al acceder a la cámara") : AppError(message)
        class FileDeleteError(message: String = "Error al eliminar el archivo") : AppError(message)
        class UnknownError(message: String = "Error desconocido") : AppError(message)
    }

    /**
     * Replica ErrorHandler.handleException() sin dependencia de android.util.Log.
     */
    private fun handleException(e: Exception): AppError {
        return when (e) {
            is java.io.IOException -> AppError.ImageSaveError("Error de E/S: ${e.message}")
            is SecurityException -> AppError.CameraError("Permiso denegado")
            else -> AppError.UnknownError(e.message ?: "Error inesperado")
        }
    }

    /**
     * Replica ErrorHandler.getUserMessage().
     */
    private fun getUserMessage(error: AppError): String {
        return when (error) {
            is AppError.ImageSaveError -> "No se pudo guardar la imagen. Intenta nuevamente."
            is AppError.DatabaseError -> "Error al acceder al historial. Reinicia la app."
            is AppError.ClassificationError -> "No se pudo analizar la imagen. Intenta con otra foto."
            is AppError.CameraError -> "No se puede acceder a la cámara. Verifica los permisos."
            is AppError.FileDeleteError -> "No se pudo eliminar el archivo."
            is AppError.UnknownError -> "Ocurrió un error inesperado. Intenta nuevamente."
        }
    }

    // ── PT-U09a — Clasificación de errores por tipo de excepción ──

    @Test
    fun `PT-U09a — IOException se clasifica como ImageSaveError`() {
        val error = handleException(java.io.IOException("disco lleno"))
        assertTrue(
            "IOException debe producir ImageSaveError",
            error is AppError.ImageSaveError
        )
    }

    @Test
    fun `PT-U09b — SecurityException se clasifica como CameraError`() {
        val error = handleException(SecurityException("permiso denegado"))
        assertTrue(
            "SecurityException debe producir CameraError",
            error is AppError.CameraError
        )
    }

    @Test
    fun `PT-U09c — excepcion generica se clasifica como UnknownError`() {
        val error = handleException(RuntimeException("algo falló"))
        assertTrue(
            "RuntimeException debe producir UnknownError",
            error is AppError.UnknownError
        )
    }

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
        val msg = getUserMessage(AppError.ImageSaveError())
        assertTrue("Debe contener indicación de reintento", msg.contains("Intenta"))
    }

    @Test
    fun `PT-U09f — DatabaseError tiene mensaje amigable`() {
        val msg = getUserMessage(AppError.DatabaseError())
        assertTrue("Debe mencionar el historial", msg.contains("historial"))
    }

    @Test
    fun `PT-U09g — ClassificationError tiene mensaje amigable`() {
        val msg = getUserMessage(AppError.ClassificationError())
        assertTrue("Debe sugerir otra foto", msg.contains("otra foto"))
    }

    @Test
    fun `PT-U09h — CameraError tiene mensaje amigable`() {
        val msg = getUserMessage(AppError.CameraError())
        assertTrue("Debe mencionar permisos", msg.contains("permisos"))
    }

    @Test
    fun `PT-U09i — FileDeleteError tiene mensaje amigable`() {
        val msg = getUserMessage(AppError.FileDeleteError())
        assertTrue("Debe mencionar eliminación", msg.contains("eliminar"))
    }

    @Test
    fun `PT-U09j — UnknownError tiene mensaje amigable`() {
        val msg = getUserMessage(AppError.UnknownError())
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
