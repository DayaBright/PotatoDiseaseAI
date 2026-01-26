package com.tesis.potatodiseaseai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    
    /**
     * Guarda una imagen en el almacenamiento interno de la app
     * @return URI de la imagen guardada
     */
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri): Uri? {
        return try {
            // Leer bitmap desde URI
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
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
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Elimina una imagen del almacenamiento interno
     */
    fun deleteImage(imageUri: Uri): Boolean {
        return try {
            val file = File(imageUri.path ?: return false)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
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
}