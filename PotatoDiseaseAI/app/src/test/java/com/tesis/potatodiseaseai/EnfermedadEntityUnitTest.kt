package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test
import com.tesis.potatodiseaseai.data.database.EnfermedadEntity

/**
 * ==========================================================================
 *  PT-U10 — Entidad EnfermedadEntity (listas separadas por "|")
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *  RF cubierto: RF-14 (biblioteca de enfermedades con info detallada)
 *  HU cubierta: HU-09 (guía educativa de enfermedades)
 *
 *  Verifica que los métodos de utilidad de la propia EnfermedadEntity
 *  (getPrevencionList, getControlQuimicoList, getControlBiologicoList)
 *  descomponen correctamente las cadenas separadas por "|".
 */
class EnfermedadEntityUnitTest {

    private fun getEntity(prevencion: String = "", quimico: String = "", biologico: String = ""): EnfermedadEntity {
        return EnfermedadEntity(
            labelCnn = "test",
            nombre = "test",
            agenteCausal = "test",
            tipoAgente = "test",
            patronVisual = "test",
            impacto = "test",
            prevencion = prevencion,
            controlQuimico = quimico,
            controlBiologico = biologico,
            imagenReferencia = "test",
            imagenGradcam = "test",
            fuentes = "test"
        )
    }

    // ── PT-U10a — Cadena con múltiples ítems separados por "|" ──

    @Test
    fun `PT-U10a — cadena con 3 items separados por pipe devuelve lista de 3`() {
        val input = "Uso de variedades resistentes|Eliminación de rastrojos|Rotación de cultivos"
        val result = getEntity(prevencion = input).getPrevencionList()
        assertEquals("Debe producir 3 elementos", 3, result.size)
        assertEquals("Uso de variedades resistentes", result[0])
        assertEquals("Eliminación de rastrojos", result[1])
        assertEquals("Rotación de cultivos", result[2])
    }

    // ── PT-U10b — Cadena con un solo ítem ──

    @Test
    fun `PT-U10b — cadena sin pipe devuelve lista de 1 elemento`() {
        val input = "Aplicar fungicidas preventivos"
        val result = getEntity(prevencion = input).getPrevencionList()
        assertEquals("Debe producir 1 elemento", 1, result.size)
        assertEquals("Aplicar fungicidas preventivos", result[0])
    }

    // ── PT-U10c — Cadena vacía ──

    @Test
    fun `PT-U10c — cadena vacia devuelve lista vacia`() {
        val result = getEntity(prevencion = "").getPrevencionList()
        assertTrue("Cadena vacía debe producir lista vacía", result.isEmpty())
    }

    // ── PT-U10d — Cadena con espacios alrededor de los pipes ──

    @Test
    fun `PT-U10d — espacios alrededor de pipes se recortan`() {
        val input = " Mancozeb | Metalaxyl | Clorotalonil "
        val result = getEntity(quimico = input).getControlQuimicoList()
        assertEquals(3, result.size)
        assertEquals("Mancozeb", result[0])
        assertEquals("Metalaxyl", result[1])
        assertEquals("Clorotalonil", result[2])
    }

    // ── PT-U10e — Cadena con pipes consecutivos (ítems vacíos) ──

    @Test
    fun `PT-U10e — pipes consecutivos se filtran como vacios`() {
        val input = "Trichoderma spp.||Bacillus spp."
        val result = getEntity(biologico = input).getControlBiologicoList()
        assertEquals("Los ítems vacíos se deben filtrar", 2, result.size)
        assertEquals("Trichoderma spp.", result[0])
        assertEquals("Bacillus spp.", result[1])
    }

    // ── PT-U10f — Simulación completa de EnfermedadEntity ──

    @Test
    fun `PT-U10f — datos completos de enfermedad tienen listas validas`() {
        // Simular datos de Tizón tardío como están en la BD
        val entity = getEntity(
            prevencion = "Uso de variedades resistentes|Eliminación de rastrojos|Evitar riego por aspersión",
            quimico = "Mancozeb|Metalaxyl",
            biologico = "Trichoderma spp.|Bacillus spp."
        )

        val prevList = entity.getPrevencionList()
        val quimList = entity.getControlQuimicoList()
        val bioList = entity.getControlBiologicoList()

        assertTrue("Prevención debe tener ítems", prevList.isNotEmpty())
        assertTrue("Control químico debe tener ítems", quimList.isNotEmpty())
        assertTrue("Control biológico debe tener ítems", bioList.isNotEmpty())

        // Verificar que ningún ítem está vacío
        (prevList + quimList + bioList).forEach { item ->
            assertTrue(
                "Ningún ítem debe estar vacío o en blanco: '$item'",
                item.isNotBlank()
            )
        }
    }

    // ── PT-U10g — "No requiere" se detecta como marcador especial ──

    @Test
    fun `PT-U10g — cadena que empieza con 'No requiere' se identifica correctamente`() {
        val input = "No requiere tratamiento químico"
        val result = getEntity(quimico = input).getControlQuimicoList()
        assertEquals(1, result.size)
        assertTrue(
            "Debe detectarse que empieza con 'No requiere'",
            result.any { it.startsWith("No requiere") }
        )
    }
}
