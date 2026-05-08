package com.tesis.potatodiseaseai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla ENFERMEDADES — Biblioteca estática de patologías de papa.
 * Se precarga con datos semilla al crear la base de datos.
 *
 * Campos verificados por reportes académicos:
 *  - agenteCausal: organismo que causa la enfermedad
 *  - tipoAgente: clasificación del agente (hongo, oomiceto, virus, etc.)
 *  - patronVisual: patrón visual observable en la hoja
 *  - impacto: consecuencias económicas/productivas
 *  - prevencion: medidas preventivas recomendadas
 *  - controlQuimico: productos químicos recomendados
 *  - controlBiologico: agentes biológicos o prácticas de biocontrol
 *
 * imagenReferencia / imagenGradcam: nombres de recursos drawable
 * (ej. "lateblight_normal"). Vacíos hasta añadir las imágenes.
 */
@Entity(tableName = "enfermedades")
data class EnfermedadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Etiqueta del modelo CNN (ej. "late blight", "healthy") */
    val labelCnn: String,
    val nombre: String,
    val agenteCausal: String,
    /** Tipo de agente causal (ej. "Oomiceto", "Hongo", "Virus") */
    val tipoAgente: String,
    /** Patrón visual característico en la hoja */
    val patronVisual: String,
    val impacto: String,
    /** Medidas preventivas recomendadas */
    val prevencion: String,
    /** Productos o tratamientos químicos recomendados */
    val controlQuimico: String,
    /** Agentes biológicos o prácticas de biocontrol */
    val controlBiologico: String,
    /** Nombre del recurso drawable para la hoja de referencia (normal) */
    val imagenReferencia: String,
    /** Nombre del recurso drawable para la imagen Grad-CAM representativa */
    val imagenGradcam: String,
    val fuentes: String
) {
    /** Devuelve las medidas de prevención como lista separada por '|' */
    fun getPrevencionList(): List<String> =
        prevencion.split("|").map { it.trim() }.filter { it.isNotBlank() }

    /** Devuelve el control químico como lista separada por '|' */
    fun getControlQuimicoList(): List<String> =
        controlQuimico.split("|").map { it.trim() }.filter { it.isNotBlank() }

    /** Devuelve el control biológico como lista separada por '|' */
    fun getControlBiologicoList(): List<String> =
        controlBiologico.split("|").map { it.trim() }.filter { it.isNotBlank() }
}
