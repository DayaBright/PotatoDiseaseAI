package com.tesis.potatodiseaseai.utils

import android.util.Log

sealed class AppError(val message: String) {
    class ImageSaveError(message: String = "Error al guardar la imagen") : AppError(message)
    class DatabaseError(message: String = "Error en la base de datos") : AppError(message)
    class ClassificationError(message: String = "Error al clasificar la imagen") : AppError(message)
    class CameraError(message: String = "Error al acceder a la cámara") : AppError(message)
    class FileDeleteError(message: String = "Error al eliminar el archivo") : AppError(message)
    class UnknownError(message: String = "Error desconocido") : AppError(message)
}

object ErrorHandler {
    private const val TAG = "ErrorHandler"
    
    /**
     * Maneja excepciones y retorna un AppError
     */
    fun handleException(e: Exception, context: String = ""): AppError {
        Log.e(TAG, "Error en $context: ${e.message}", e)
        
        return when (e) {
            is java.io.IOException -> AppError.ImageSaveError("Error de E/S: ${e.message}")
            is android.database.SQLException -> AppError.DatabaseError("Error de BD: ${e.message}")
            is SecurityException -> AppError.CameraError("Permiso denegado")
            else -> AppError.UnknownError(e.message ?: "Error inesperado")
        }
    }
    
    /**
     * Convierte AppError a mensaje amigable para el usuario
     */
    fun getUserMessage(error: AppError): String {
        return when (error) {
            is AppError.ImageSaveError -> "No se pudo guardar la imagen. Intenta nuevamente."
            is AppError.DatabaseError -> "Error al acceder al historial. Reinicia la app."
            is AppError.ClassificationError -> "No se pudo analizar la imagen. Intenta con otra foto."
            is AppError.CameraError -> "No se puede acceder a la cámara. Verifica los permisos."
            is AppError.FileDeleteError -> "No se pudo eliminar el archivo."
            is AppError.UnknownError -> "Ocurrió un error inesperado. Intenta nuevamente."
        }
    }
}