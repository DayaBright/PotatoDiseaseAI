package com.tesis.potatodiseaseai.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDao {
    
    @Insert
    suspend fun insert(detection: DetectionEntity): Long
    
    @Delete
    suspend fun delete(detection: DetectionEntity)
    
    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    fun getAllDetections(): Flow<List<DetectionEntity>>
    
    @Query("SELECT * FROM detections WHERE id = :id")
    suspend fun getDetectionById(id: Long): DetectionEntity?
    
    @Query("DELETE FROM detections WHERE id = :id")
    suspend fun deleteById(id: Long)
}