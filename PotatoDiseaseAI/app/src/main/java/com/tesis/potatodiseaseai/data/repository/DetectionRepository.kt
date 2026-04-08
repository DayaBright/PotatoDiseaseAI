package com.tesis.potatodiseaseai.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.DetectionEntity
import com.tesis.potatodiseaseai.utils.FileUtils
import kotlinx.coroutines.flow.Flow

class DetectionRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val detectionDao = database.detectionDao()
    
    companion object {
        private const val TAG = "DetectionRepository"
    }
    
    /**
     * Obtiene todas las detecciones como Flow
     */
    fun getAllDetections(): Flow<List<DetectionEntity>> {
        return detectionDao.getAllDetections()
    }
    
    /**
     * Inserta una nueva detección
     */
    suspend fun insertDetection(detection: DetectionEntity): Long {
        return detectionDao.insert(detection)
    }
    
    /**
     * Elimina una detección y su imagen asociada
     */
    suspend fun deleteDetection(detection: DetectionEntity): Boolean {
        return deleteDetectionInternal(detection.id, detection.imageUri)
    }
    
    /**
     * Elimina una detección por ID
     */
    suspend fun deleteDetectionById(detectionId: Long, imageUri: String): Boolean {
        return deleteDetectionInternal(detectionId, imageUri)
    }
    
    // ✅ Método privado centralizado
    private suspend fun deleteDetectionInternal(
        detectionId: Long,
        imageUri: String
    ): Boolean {
        return try {
            detectionDao.deleteById(detectionId)
            FileUtils.deleteImage(Uri.parse(imageUri))
            Log.d(TAG, "✓ Detección eliminada: $detectionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error eliminando detección: ${e.message}", e)
            false
        }
    }
    
    /**
     * Obtiene el tamaño total de almacenamiento
     */
    fun getTotalStorageSize(): Double {
        return FileUtils.getTotalImagesSizeInMB(context)
    }
}