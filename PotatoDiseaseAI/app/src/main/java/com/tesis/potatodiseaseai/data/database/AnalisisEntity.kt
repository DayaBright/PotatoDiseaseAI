package com.tesis.potatodiseaseai.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla ANALISIS — Historial de clasificaciones del usuario.
 *
 * Cada registro almacena la imagen capturada, la enfermedad detectada
 * (por FK a enfermedades) y la precisión del modelo.
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
    val precision: Float,
    val fechaHora: Long = System.currentTimeMillis()
)
