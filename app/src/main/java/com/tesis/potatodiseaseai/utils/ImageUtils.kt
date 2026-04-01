package com.tesis.potatodiseaseai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {
    
    private const val TAG = "ImageUtils"
    
    /**
     * Corrige la rotación de una imagen según su información EXIF
     */
    fun fixImageRotation(context: Context, imageUri: Uri): Uri {
        var bitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null
        
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap == null) {
                AppLogger.error(TAG, "Error: No se pudo decodificar el bitmap")  // ✅ CAMBIAR
                return imageUri
            }

            val exif = context.contentResolver.openInputStream(imageUri)?.use {
                ExifInterface(it)
            }
            
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            ) ?: ExifInterface.ORIENTATION_UNDEFINED

            AppLogger.debug(TAG, "Orientación EXIF detectada: $orientation")  // ✅ CAMBIAR

            rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    AppLogger.debug(TAG, "Rotando 90°")  // ✅ CAMBIAR
                    rotateBitmap(bitmap, 90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    AppLogger.debug(TAG, "Rotando 180°")  // ✅ CAMBIAR
                    rotateBitmap(bitmap, 180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    AppLogger.debug(TAG, "Rotando 270°")  // ✅ CAMBIAR
                    rotateBitmap(bitmap, 270f)
                }
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                    AppLogger.debug(TAG, "Volteando horizontalmente")  // ✅ CAMBIAR
                    flipBitmap(bitmap, horizontal = true)
                }
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    AppLogger.debug(TAG, "Volteando verticalmente")  // ✅ CAMBIAR
                    flipBitmap(bitmap, horizontal = false)
                }
                else -> {
                    AppLogger.debug(TAG, "Sin rotación necesaria")  // ✅ CAMBIAR
                    bitmap
                }
            }

            if (rotatedBitmap == bitmap) {
                return imageUri
            }

            val correctedFile = File(context.cacheDir, "corrected_${System.currentTimeMillis()}.jpg")
            FileOutputStream(correctedFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            AppLogger.debug(TAG, "✓ Imagen corregida guardada: ${correctedFile.absolutePath}")  // ✅ CAMBIAR
            Uri.fromFile(correctedFile)
            
        } catch (e: Exception) {
            AppLogger.error(TAG, "Error corrigiendo rotación: ${e.message}", e)  // ✅ CAMBIAR
            imageUri
        } finally {
            if (rotatedBitmap != null && rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap?.recycle()
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun flipBitmap(source: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Recorta un cuadrado centrado del bitmap.
     * Esto coincide con el cuadro guía que se muestra en la cámara,
     * asegurando que solo el área enmarcada por el usuario se envíe al clasificador.
     * @param fraction Fracción del lado menor a usar (0.0-1.0). Por defecto 0.85 (85%).
     */
    fun centerCropSquare(source: Bitmap, fraction: Float = 0.85f): Bitmap {
        val side = (minOf(source.width, source.height) * fraction.coerceIn(0.1f, 1.0f)).toInt()
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        return Bitmap.createBitmap(source, x, y, side, side)
    }
}