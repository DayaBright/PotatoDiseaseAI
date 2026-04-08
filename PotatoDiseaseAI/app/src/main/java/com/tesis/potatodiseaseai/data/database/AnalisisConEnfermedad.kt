package com.tesis.potatodiseaseai.data.database

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relación Room que une AnalisisEntity con su EnfermedadEntity correspondiente.
 * Usada por AnalisisDao con @Transaction para evitar lecturas parciales.
 */
data class AnalisisConEnfermedad(
    @Embedded val analisis: AnalisisEntity,
    @Relation(
        parentColumn = "enfermedadId",
        entityColumn = "id"
    )
    val enfermedad: EnfermedadEntity
)
