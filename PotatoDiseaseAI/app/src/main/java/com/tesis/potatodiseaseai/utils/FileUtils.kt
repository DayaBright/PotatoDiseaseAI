package com.tesis.potatodiseaseai.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
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
            val filename = "IMG_${System.currentTimeMillis()}.webp"
            val file = File(directory, filename)
            
            // Guardar bitmap
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out) // Reducido a 80% calidad en WEBP
            }
            
            Uri.fromFile(file).also {
                AppLogger.debug(TAG, "✓ Imagen guardada: ${file.absolutePath}")  // ✅ CAMBIAR
            }
        } catch (e: Exception) {
            AppLogger.error(TAG, "Error guardando imagen: ${e.message}", e)  // ✅ CAMBIAR
            null
        } finally {
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
                (file.extension == "jpg" || file.extension == "webp") && (
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

    /**
     * Copia un archivo desde la carpeta assets a la carpeta cache interna de la app.
     */
    fun copyAssetToCache(context: Context, assetName: String, destFileName: String): File? {
        return try {
            val destFile = File(context.cacheDir, destFileName)
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            AppLogger.debug(TAG, "✓ Asset copiado exitosamente a: ${destFile.absolutePath}")
            destFile
        } catch (e: Exception) {
            AppLogger.error(TAG, "Error al copiar el asset $assetName: ${e.message}", e)
            null
        }
    }

    /**
     * Copia y abre el archivo PDF desde caché usando un Intent y FileProvider.
     */
    fun openPdfFromCache(context: Context, fileName: String) {
        try {
            val file = File(context.cacheDir, fileName)

            val copiedFile = copyAssetToCache(context, "Manual_Usuario.pdf", fileName)
            if (copiedFile == null || !copiedFile.exists()) {
                Toast.makeText(context, "El archivo del manual no se encuentra disponible.", Toast.LENGTH_SHORT).show()
                return
            }

            // Generar URI seguro usando FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, copiedFile)

            // Crear intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "No se encontró una aplicación para abrir archivos PDF. Por favor, instale un lector de PDF.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            AppLogger.error(TAG, "Error al abrir el PDF: ${e.message}", e)
            Toast.makeText(context, "Error al intentar abrir el manual de usuario.", Toast.LENGTH_SHORT).show()
        }
    }
}