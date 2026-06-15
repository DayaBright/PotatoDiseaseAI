package com.tesis.potatodiseaseai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
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

            val correctedFile = File(context.cacheDir, "corrected_${System.currentTimeMillis()}.webp")
            FileOutputStream(correctedFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
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

    data class CropResult(val left: Int, val top: Int, val width: Int, val height: Int)

    /**
     * Lógica pura para el cálculo del recorte (testable en JVM)
     */
    fun calculateCropRect(
        imageW: Float,
        imageH: Float,
        screenWidth: Int,
        screenHeight: Int,
        guideFraction: Float,
        verticalOffsetDp: Float,
        density: Float
    ): CropResult {
        val scale = maxOf(screenWidth / imageW, screenHeight / imageH)
        val scaledImageW = imageW * scale
        val scaledImageH = imageH * scale

        val imageLeftOnScreen = (screenWidth - scaledImageW) / 2f
        val imageTopOnScreen = (screenHeight - scaledImageH) / 2f

        val sideOnScreen = minOf(screenWidth, screenHeight) * guideFraction
        val squareLeftOnScreen = (screenWidth - sideOnScreen) / 2f
        val verticalOffsetPx = verticalOffsetDp * density
        val squareTopOnScreen = (screenHeight - sideOnScreen) / 2f - verticalOffsetPx

        val cropLeft = (squareLeftOnScreen - imageLeftOnScreen) / scale
        val cropTop = (squareTopOnScreen - imageTopOnScreen) / scale
        val cropSide = sideOnScreen / scale

        val finalLeft = cropLeft.toInt().coerceIn(0, imageW.toInt())
        val finalTop = cropTop.toInt().coerceIn(0, imageH.toInt())
        val finalRight = (cropLeft + cropSide).toInt().coerceIn(0, imageW.toInt())
        val finalBottom = (cropTop + cropSide).toInt().coerceIn(0, imageH.toInt())

        return CropResult(
            left = finalLeft,
            top = finalTop,
            width = finalRight - finalLeft,
            height = finalBottom - finalTop
        )
    }

    /**
     * Recorta un cuadrado de la imagen mapeando las coordenadas del recuadro
     * visual (que tiene un offset y un tamaño relativo a la pantalla) a las
     * coordenadas reales de la imagen, asumiendo un escalado FILL_CENTER.
     */
    fun cropToPreviewSquare(
        source: Bitmap,
        screenWidth: Int,
        screenHeight: Int,
        guideFraction: Float = 0.85f,
        verticalOffsetDp: Float = 60f,
        density: Float
    ): Bitmap {
        val cropRect = calculateCropRect(
            source.width.toFloat(), source.height.toFloat(),
            screenWidth, screenHeight, guideFraction, verticalOffsetDp, density
        )

        if (cropRect.width <= 0 || cropRect.height <= 0) return source

        return Bitmap.createBitmap(source, cropRect.left, cropRect.top, cropRect.width, cropRect.height)
    }

    /**
     * Recorta un cuadrado centrado simple del bitmap (Legacy/Fallback)
     */
    fun centerCropSquare(source: Bitmap, fraction: Float = 0.85f): Bitmap {
        val side = (minOf(source.width, source.height) * fraction.coerceIn(0.1f, 1.0f)).toInt()
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        return Bitmap.createBitmap(source, x, y, side, side)
    }

    /**
     * Corrige la rotación del bitmap en memoria según EXIF sin guardar en disco.
     */
    fun fixRotationInMemory(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it)
            }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            ) ?: ExifInterface.ORIENTATION_UNDEFINED

            val angle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return bitmap // sin rotación → devuelve el original
            }

            rotateBitmap(bitmap, angle)
        } catch (e: Exception) {
            AppLogger.error(TAG, "Error leyendo EXIF: ${e.message}")
            bitmap
        }
    }

    data class LetterboxResult(val scaledW: Int, val scaledH: Int, val offsetX: Float, val offsetY: Float)

    /**
     * Lógica pura matemática para letterbox (testable en JVM)
     */
    fun calculateLetterbox(srcW: Float, srcH: Float, targetSize: Int): LetterboxResult {
        val scale = targetSize.toFloat() / maxOf(srcW, srcH)
        val scaledW = (srcW * scale).toInt()
        val scaledH = (srcH * scale).toInt()
        val offsetX = (targetSize - scaledW) / 2f
        val offsetY = (targetSize - scaledH) / 2f
        return LetterboxResult(scaledW, scaledH, offsetX, offsetY)
    }

    /**
     * Redimensiona un bitmap para que quepa dentro de un cuadrado de [targetSize]×[targetSize]
     * manteniendo la proporción original, y rellena los bordes vacíos con negro.
     */
    fun letterboxBitmap(source: Bitmap, targetSize: Int): Bitmap {
        val result = calculateLetterbox(source.width.toFloat(), source.height.toFloat(), targetSize)

        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawColor(android.graphics.Color.rgb(112, 122, 95))

        val scaledBitmap = source.scale(result.scaledW, result.scaledH)
        canvas.drawBitmap(scaledBitmap, result.offsetX, result.offsetY, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))

        if (scaledBitmap !== source) scaledBitmap.recycle()

        return output
    }
}