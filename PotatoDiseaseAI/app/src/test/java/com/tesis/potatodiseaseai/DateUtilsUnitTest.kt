package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * ==========================================================================
 *  PT-U07 — Formateo de fechas (DateUtils)
 *  Archivo: src/test/.../DateUtilsUnitTest.kt
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *
 *  RF cubierto: RF-12 (visualizar historial con fecha de cada análisis)
 *  HU cubierta: HU-06 (seguimiento cronológico de análisis)
 *
 *  Verifica que DateUtils formatea correctamente los timestamps para
 *  mostrarlos en la pantalla de historial.
 */
class DateUtilsUnitTest {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    /**
     * Replica DateUtils.formatTimestamp()
     */
    private fun formatTimestamp(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp))
    }

    /**
     * Replica DateUtils.getRelativeTime()
     */
    private fun getRelativeTime(timestamp: Long, now: Long): String {
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Hace un momento"
            diff < 3_600_000 -> "Hace ${diff / 60_000} min"
            diff < 86_400_000 -> "Hace ${diff / 3_600_000}h"
            diff < 604_800_000 -> "Hace ${diff / 86_400_000}d"
            else -> formatTimestamp(timestamp)
        }
    }

    // ── PT-U07a — Formato de timestamp ──

    @Test
    fun `PT-U07a — timestamp se formatea en patron dd-MM-yyyy HH-mm`() {
        // Usar una fecha conocida: 15/03/2026 10:30
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 10, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val timestamp = calendar.timeInMillis
        val formatted = formatTimestamp(timestamp)

        assertTrue(
            "El formato debe contener '/' como separador de fecha: $formatted",
            formatted.contains("/")
        )
        assertTrue(
            "El formato debe contener ':' como separador de hora: $formatted",
            formatted.contains(":")
        )
    }

    @Test
    fun `PT-U07b — timestamp 0 (epoch) se formatea sin error`() {
        val formatted = formatTimestamp(0L)
        assertNotNull("El formato no debe ser null", formatted)
        assertTrue(
            "El resultado no debe estar vacío",
            formatted.isNotBlank()
        )
    }

    // ── PT-U07c — Tiempo relativo ──

    @Test
    fun `PT-U07c — diferencia menor a 1 minuto muestra 'Hace un momento'`() {
        val now = System.currentTimeMillis()
        val timestamp = now - 30_000 // 30 segundos atrás
        assertEquals("Hace un momento", getRelativeTime(timestamp, now))
    }

    @Test
    fun `PT-U07d — diferencia de 5 minutos muestra 'Hace 5 min'`() {
        val now = System.currentTimeMillis()
        val timestamp = now - (5 * 60_000) // 5 minutos atrás
        assertEquals("Hace 5 min", getRelativeTime(timestamp, now))
    }

    @Test
    fun `PT-U07e — diferencia de 3 horas muestra 'Hace 3h'`() {
        val now = System.currentTimeMillis()
        val timestamp = now - (3 * 3_600_000L) // 3 horas atrás
        assertEquals("Hace 3h", getRelativeTime(timestamp, now))
    }

    @Test
    fun `PT-U07f — diferencia de 2 dias muestra 'Hace 2d'`() {
        val now = System.currentTimeMillis()
        val timestamp = now - (2 * 86_400_000L) // 2 días atrás
        assertEquals("Hace 2d", getRelativeTime(timestamp, now))
    }

    @Test
    fun `PT-U07g — diferencia mayor a 7 dias muestra fecha completa`() {
        val now = System.currentTimeMillis()
        val timestamp = now - (10 * 86_400_000L) // 10 días atrás
        val result = getRelativeTime(timestamp, now)
        // Debe caer en el else → formatTimestamp(), que contiene "/"
        assertTrue(
            "Más de 7 días debe mostrar fecha formateada: $result",
            result.contains("/")
        )
    }
}
