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
        precision: Float
    ): Long {
        val enfermedad = enfermedadDao.getByLabel(labelCnn)
        if (enfermedad == null) {
            // El label no existe en la BD de enfermedades (ej: "z no potato").
            // NO guardar — retornar -1L como indicador de rechazo silencioso.
            Log.w(TAG, "⚠ Label '$labelCnn' no existe en BD de enfermedades — descartado sin guardar")
            return -1L
        }
        val analisis = AnalisisEntity(
            enfermedadId = enfermedad.id,
            imagenCapturada = imagenUri,
            precision = precision
        )
        return analisisDao.insert(analisis)
    }

    suspend fun deleteAnalisis(analisis: AnalisisConEnfermedad): Boolean =
        deleteInternal(analisis.analisis.id, analisis.analisis.imagenCapturada)

    suspend fun deleteAnalisisById(id: Long, imageUri: String): Boolean =
        deleteInternal(id, imageUri)

    /**
     * Elimina todos los análisis: primero borra las imágenes del almacenamiento
     * y luego limpia la tabla completa.
     */
    suspend fun deleteAllAnalisis(): Boolean {
        return try {
            val allAnalisis = analisisDao.getAllAnalisisList()
            allAnalisis.forEach { item ->
                FileUtils.deleteImage(Uri.parse(item.analisis.imagenCapturada))
            }
            analisisDao.deleteAll()
            Log.d(TAG, "✓ Todos los análisis eliminados (${allAnalisis.size} registros)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error eliminando todos los análisis: ${e.message}", e)
            false
        }
    }

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
