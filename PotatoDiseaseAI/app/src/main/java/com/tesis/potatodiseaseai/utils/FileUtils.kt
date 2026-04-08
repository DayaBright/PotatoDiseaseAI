package com.tesis.potatodiseaseai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    
    private const val TAG = "FileUtils"
    private const val MAX_IMAGE_DIMENSION = 1024
    
    /**
     * Guarda una imagen en el almacenamiento interno de la app
     */
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri): Uri? {
        var bitmap: Bitmap? = null
        return try {
            // Leer bitmap desde URI con reducción de tamaño
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            // Calcular factor de escala
            val scaleFactor = calculateInSampleSize(options, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
            
            // Decodificar con escala
            val inputStream2 = context.contentResolver.openInputStream(sourceUri)
            options.inJustDecodeBounds = false
            options.inSampleSize = scaleFactor
            bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
            inputStream2?.close()
            
            if (bitmap == null) {
                Log.e(TAG, "Error: Bitmap es null después de decodificar")
                return null
            }
            
            // Crear directorio si no existe
            val directory = File(context.filesDir, "detections")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            // Crear archivo con timestamp
            val filename = "IMG_${System.currentTimeMillis()}.jpg"
            val file = File(directory, filename)
            
            // Guardar bitmap
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) // Reducido a 85% calidad
            }
            
            Uri.fromFile(file).also {
                AppLogger.debug(TAG, "✓ Imagen guardada: ${file.absolutePath}")  // ✅ CAMBIAR
            }
        } catch (e: Exception) {
            AppLogger.error(TAG, "Error guardando imagen: ${e.message}", e)  // ✅ CAMBIAR
            null
        } finally {
            // ✅ CRÍTICO: Reciclar bitmap
            bitmap?.recycle()
        }
    }
    
    /**
     * Calcula el factor de escala para reducir el tamaño de la imagen
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
    
    /**
     * Elimina una imagen del almacenamiento interno
     */
    fun deleteImage(imageUri: Uri): Boolean {
        return try {
            val file = File(imageUri.path ?: return false)
            val deleted = file.delete()
            if (deleted) {
                AppLogger.debug(TAG, "✓ Imagen eliminada: ${file.absolutePath}")  // ✅ CAMBIAR
            } else {
                AppLogger.warning(TAG, "No se pudo eliminar: ${file.absolutePath}")  // ✅ CAMBIAR
            }
            deleted
        } catch (e: Exception) {
            AppLogger.error(TAG, "Error eliminando imagen: ${e.message}", e)  // ✅ CAMBIAR
            false
        }
    }
    
    /**
     * Obtiene el tamaño de todas las imágenes guardadas
     */
    fun getTotalImagesSizeInMB(context: Context): Double {
        val directory = File(context.filesDir, "detections")
        if (!directory.exists()) return 0.0
        
        val totalBytes = directory.listFiles()?.sumOf { it.length() } ?: 0
        return totalBytes / (1024.0 * 1024.0)
    }
    
    /**
     * Limpia archivos temporales antiguos
     */
    fun cleanTempFiles(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val tempFiles = cacheDir.listFiles { file ->
                file.extension == "jpg" && (
                    file.name.startsWith("temp_") ||
                    file.name.startsWith("corrected_") ||
                    file.name.startsWith("cropped_")
                )
            }
            
            tempFiles?.forEach { file ->
                if (file.delete()) {
                    AppLogger.debug(TAG, "✓ Archivo temporal eliminado: ${file.name}")  // ✅ CAMBIAR
                }
            }
        } catch (e: Exception) {
            AppLogger.error(TAG, "Error limpiando archivos temporales: ${e.message}", e)  // ✅ CAMBIAR
        }
    }
}