package com.tesis.potatodiseaseai.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalisisDao {

    @Insert
    suspend fun insert(analisis: AnalisisEntity): Long

    @Delete
    suspend fun delete(analisis: AnalisisEntity)

    @Query("DELETE FROM analisis WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM analisis")
    suspend fun deleteAll()

    /** Todos los análisis con su enfermedad relacionada, orden cronológico inverso */
    @Transaction
    @Query("SELECT * FROM analisis ORDER BY fechaHora DESC")
    fun getAllAnalisis(): Flow<List<AnalisisConEnfermedad>>

    /** Versión suspend (no Flow) para obtener la lista antes de eliminar */
    @Transaction
    @Query("SELECT * FROM analisis ORDER BY fechaHora DESC")
    suspend fun getAllAnalisisList(): List<AnalisisConEnfermedad>

    @Transaction
    @Query("SELECT * FROM analisis WHERE id = :id")
    suspend fun getById(id: Long): AnalisisConEnfermedad?
}
