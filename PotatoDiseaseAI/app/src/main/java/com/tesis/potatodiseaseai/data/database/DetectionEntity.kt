package com.tesis.potatodiseaseai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detections")
data class DetectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageUri: String,
    val disease: String,
    val diseaseName: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)