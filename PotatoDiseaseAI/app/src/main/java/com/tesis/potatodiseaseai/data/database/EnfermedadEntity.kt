package com.tesis.potatodiseaseai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla ENFERMEDADES — Biblioteca estática de patologías de papa.
 * Se precarga con datos semilla al crear la base de datos.
 *
 * imagenReferencia / imagenGradcam: nombres de recursos drawable
 * (ej. "img_late_blight_normal"). Vacíos hasta añadir las imágenes.
 */
@Entity(tableName = "enfermedades")
data class EnfermedadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Etiqueta del modelo CNN (ej. "late blight", "healthy") */
    val labelCnn: String,
    val nombre: String,
    val agenteCausal: String,
    val impacto: String,
    val manifestacionesVisuales: String,
    val signosClave: String,
    /** Recomendaciones de manejo separadas por '|' */
    val recomendaciones: String,
    /** Nombre del recurso drawable para la hoja de referencia (normal) */
    val imagenReferencia: String,
    /** Nombre del recurso drawable para la imagen Grad-CAM representativa */
    val imagenGradcam: String,
    val fuentes: String
) {
    fun getRecomendacionesList(): List<String> =
        recomendaciones.split("|").map { it.trim() }.filter { it.isNotBlank() }
}
