package com.tesis.potatodiseaseai.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla ANALISIS — Historial de clasificaciones del usuario.
 *
 * imagenGradcamReal: URI del Grad-CAM generado en tiempo real por el modelo
 * para ese análisis específico. Diferente al imagenGradcam de EnfermedadEntity,
 * que es la imagen educativa/representativa de la patología.
 */
@Entity(
    tableName = "analisis",
    foreignKeys = [
        ForeignKey(
            entity = EnfermedadEntity::class,
            parentColumns = ["id"],
            childColumns = ["enfermedadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("enfermedadId")]
)
data class AnalisisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val enfermedadId: Long,
    val imagenCapturada: String,
    /** URI del Grad-CAM generado en tiempo real (null si no está disponible) */
    val imagenGradcamReal: String? = null,
    val precision: Float,
    val fechaHora: Long = System.currentTimeMillis()
)
