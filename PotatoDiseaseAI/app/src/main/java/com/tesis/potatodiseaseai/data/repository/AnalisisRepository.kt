package com.tesis.potatodiseaseai.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tesis.potatodiseaseai.data.database.AnalisisConEnfermedad
import com.tesis.potatodiseaseai.data.database.AnalisisEntity
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.EnfermedadEntity
import com.tesis.potatodiseaseai.utils.FileUtils
import kotlinx.coroutines.flow.Flow

class AnalisisRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val analisisDao = database.analisisDao()
    private val enfermedadDao = database.enfermedadDao()

    companion object {
        private const val TAG = "AnalisisRepository"
    }

    // ── Historial ────────────────────────────────────────────────────────────

    fun getAllAnalisis(): Flow<List<AnalisisConEnfermedad>> =
        analisisDao.getAllAnalisis()

    /**
     * Guarda un nuevo análisis vinculándolo por labelCnn a la tabla enfermedades.
     * Retorna el ID del registro insertado.
     */
    suspend fun insertAnalisis(
        labelCnn: String,
        imagenUri: String,
        precision: Float,
        imagenGradcamReal: String? = null
    ): Long {
        val enfermedad = enfermedadDao.getByLabel(labelCnn)
        val enfermedadId = enfermedad?.id ?: run {
            Log.w(TAG, "⚠ Enfermedad '$labelCnn' no encontrada en BD — usando fallback id=1")
            1L
        }
        val analisis = AnalisisEntity(
            enfermedadId = enfermedadId,
            imagenCapturada = imagenUri,
            imagenGradcamReal = imagenGradcamReal,
            precision = precision
        )
        return analisisDao.insert(analisis)
    }

    suspend fun deleteAnalisis(analisis: AnalisisConEnfermedad): Boolean =
        deleteInternal(analisis.analisis.id, analisis.analisis.imagenCapturada)

    suspend fun deleteAnalisisById(id: Long, imageUri: String): Boolean =
        deleteInternal(id, imageUri)

    private suspend fun deleteInternal(id: Long, imageUri: String): Boolean {
        return try {
            analisisDao.deleteById(id)
            FileUtils.deleteImage(Uri.parse(imageUri))
            Log.d(TAG, "✓ Análisis eliminado: $id")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error eliminando análisis: ${e.message}", e)
            false
        }
    }

    // ── Biblioteca de enfermedades ────────────────────────────────────────────

    fun getAllEnfermedades() = enfermedadDao.getAllEnfermedades()

    suspend fun getEnfermedadByLabel(labelCnn: String): EnfermedadEntity? =
        enfermedadDao.getByLabel(labelCnn)

    // ── Almacenamiento ────────────────────────────────────────────────────────

    fun getTotalStorageSize(): Double = FileUtils.getTotalImagesSizeInMB(context)
}
