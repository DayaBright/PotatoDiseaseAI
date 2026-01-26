package com.tesis.potatodiseaseai.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    
    // ✅ Singleton: Una sola instancia para toda la app
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    /**
     * Formatea un timestamp a formato legible
     */
    fun formatTimestamp(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp))
    }
    
    /**
     * Obtiene la fecha actual formateada
     */
    fun getCurrentDate(): String {
        return dateFormatter.format(Date())
    }
    
    /**
     * Formatea la diferencia de tiempo (ej: "Hace 2 horas")
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Hace un momento"
            diff < 3_600_000 -> "Hace ${diff / 60_000} min"
            diff < 86_400_000 -> "Hace ${diff / 3_600_000}h"
            diff < 604_800_000 -> "Hace ${diff / 86_400_000}d"
            else -> formatTimestamp(timestamp)
        }
    }
}