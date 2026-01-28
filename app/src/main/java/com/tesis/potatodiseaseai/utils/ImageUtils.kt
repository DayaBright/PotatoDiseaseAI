package com.tesis.potatodiseaseai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {
    
    private const val TAG = "ImageUtils"
    
    /**
     * Corrige la rotación de una imagen según su información EXIF
     * @param context Contexto de la aplicación
     * @param imageUri URI de la imagen a corregir
     * @return URI de la imagen corregida
     */
    fun fixImageRotation(context: Context, imageUri: Uri): Uri {
        var bitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null
        
        return try {
            // 1. Cargar bitmap original
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap == null) {
                Log.e(TAG, "Error: No se pudo decodificar el bitmap")
                return imageUri
            }

            // 2. Leer orientación EXIF
            val exif = context.contentResolver.openInputStream(imageUri)?.use {
                ExifInterface(it)
            }
            
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            ) ?: ExifInterface.ORIENTATION_UNDEFINED

            Log.d(TAG, "Orientación EXIF detectada: $orientation")

            // 3. Rotar bitmap según EXIF
            rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    Log.d(TAG, "Rotando 90°")
                    rotateBitmap(bitmap, 90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    Log.d(TAG, "Rotando 180°")
                    rotateBitmap(bitmap, 180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    Log.d(TAG, "Rotando 270°")
                    rotateBitmap(bitmap, 270f)
                }
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                    Log.d(TAG, "Volteando horizontalmente")
                    flipBitmap(bitmap, horizontal = true)
                }
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    Log.d(TAG, "Volteando verticalmente")
                    flipBitmap(bitmap, horizontal = false)
                }
                else -> {
                    Log.d(TAG, "Sin rotación necesaria")
                    bitmap
                }
            }

            // 4. Si no hubo rotación, devolver URI original
            if (rotatedBitmap == bitmap) {
                return imageUri
            }

            // 5. Guardar bitmap corregido en caché
            val correctedFile = File(context.cacheDir, "corrected_${System.currentTimeMillis()}.jpg")
            FileOutputStream(correctedFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            Log.d(TAG, "Imagen corregida guardada: ${correctedFile.absolutePath}")
            Uri.fromFile(correctedFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error corrigiendo rotación: ${e.message}", e)
            imageUri // Retornar URI original si hay error
        } finally {
            // ✅ CRÍTICO: Reciclar bitmaps para liberar memoria
            if (rotatedBitmap != null && rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap?.recycle()
        }
    }

    /**
     * Rota un bitmap por el ángulo especificado
     */
    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix().apply { 
            postRotate(angle) 
        }
        return Bitmap.createBitmap(
            source, 
            0, 
            0, 
            source.width, 
            source.height, 
            matrix, 
            true
        )
    }

    /**
     * Voltea un bitmap horizontal o verticalmente
     */
    private fun flipBitmap(source: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) {
                postScale(-1f, 1f) // Flip horizontal
            } else {
                postScale(1f, -1f) // Flip vertical
            }
        }
        return Bitmap.createBitmap(
            source, 
            0, 
            0, 
            source.width, 
            source.height, 
            matrix, 
            true
        )
    }
}