package com.tesis.potatodiseaseai.data.repository

import android.content.Context
import android.net.Uri
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.DetectionEntity
import com.tesis.potatodiseaseai.utils.FileUtils
import kotlinx.coroutines.flow.Flow

class DetectionRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val detectionDao = database.detectionDao()
    
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
        return try {
            detectionDao.delete(detection)
            FileUtils.deleteImage(Uri.parse(detection.imageUri))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Elimina una detección por ID
     */
    suspend fun deleteDetectionById(detectionId: Long, imageUri: String): Boolean {
        return try {
            detectionDao.deleteById(detectionId)
            FileUtils.deleteImage(Uri.parse(imageUri))
            true
        } catch (e: Exception) {
            e.printStackTrace()
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