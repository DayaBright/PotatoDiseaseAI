package com.tesis.potatodiseaseai.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EnfermedadDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(enfermedades: List<EnfermedadEntity>)

    /** Todas las enfermedades ordenadas por nombre (para HelpScreen) */
    @Query("SELECT * FROM enfermedades ORDER BY nombre ASC")
    fun getAllEnfermedades(): Flow<List<EnfermedadEntity>>

    /** Busca una enfermedad por la etiqueta del modelo CNN */
    @Query("SELECT * FROM enfermedades WHERE labelCnn = :label LIMIT 1")
    suspend fun getByLabel(label: String): EnfermedadEntity?

    @Query("SELECT COUNT(*) FROM enfermedades")
    suspend fun count(): Int
}
