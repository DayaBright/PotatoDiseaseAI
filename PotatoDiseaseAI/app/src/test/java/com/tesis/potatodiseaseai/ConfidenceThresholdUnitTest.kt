package com.tesis.potatodiseaseai

import org.junit.Assert.*
import org.junit.Test
import com.tesis.potatodiseaseai.utils.ConfidenceUtils

/**
 * ==========================================================================
 *  PT-U06 — Umbral de confianza y lógica de baja confianza
 *  Archivo: src/test/.../ConfidenceThresholdUnitTest.kt
 *  Se ejecutan en la JVM local, sin dispositivo ni emulador.
 * ==========================================================================
 *
 *  RF cubiertos: RF-09 (recomendaciones ≥70%), RF-10 (tips <70%)
 *  HU cubiertas: HU-04 (recomendaciones de manejo), HU-05 (tips baja confianza)
 *
 *  Verifica la lógica real del sistema implementada en ConfidenceUtils
 *  basada en el umbral de confianza del 70%.
 */
class ConfidenceThresholdUnitTest {

    // ── PT-U06a — Confianza exactamente en el umbral (70%) ──

    @Test
    fun `PT-U06a — confianza de 0,70 NO es baja confianza`() {
        assertFalse(
            "0.70f debe considerarse confianza suficiente",
            ConfidenceUtils.isLowConfidence(0.70f)
        )
    }

    @Test
    fun `PT-U06b — confianza de 0,70 SI se guarda en historial`() {
        assertTrue(
            "0.70f debe guardarse en historial",
            ConfidenceUtils.shouldSaveToHistory(0.70f)
        )
    }

    // ── PT-U06c — Confianza alta (por encima del umbral) ──

    @Test
    fun `PT-U06c — confianza de 0,95 NO es baja confianza`() {
        assertFalse(
            "0.95f es confianza alta",
            ConfidenceUtils.isLowConfidence(0.95f)
        )
    }

    @Test
    fun `PT-U06d — confianza de 0,85 SI se guarda en historial`() {
        assertTrue(
            "0.85f debe guardarse en historial",
            ConfidenceUtils.shouldSaveToHistory(0.85f)
        )
    }

    // ── PT-U06e — Confianza baja (por debajo del umbral) ──

    @Test
    fun `PT-U06e — confianza de 0,69 ES baja confianza`() {
        assertTrue(
            "0.69f debe considerarse confianza baja",
            ConfidenceUtils.isLowConfidence(0.69f)
        )
    }

    @Test
    fun `PT-U06f — confianza de 0,69 NO se guarda en historial`() {
        assertFalse(
            "0.69f no debe guardarse en historial",
            ConfidenceUtils.shouldSaveToHistory(0.69f)
        )
    }

    @Test
    fun `PT-U06g — confianza de 0,50 ES baja confianza`() {
        assertTrue(
            "0.50f debe considerarse confianza baja",
            ConfidenceUtils.isLowConfidence(0.50f)
        )
    }

    // ── PT-U06h — Confianza en casos extremos ──

    @Test
    fun `PT-U06h — confianza de 0,0 es baja confianza`() {
        assertTrue(
            "0.0f debe ser confianza baja",
            ConfidenceUtils.isLowConfidence(0.0f)
        )
    }

    @Test
    fun `PT-U06i — confianza de 1,0 no es baja confianza`() {
        assertFalse(
            "1.0f es la confianza máxima posible",
            ConfidenceUtils.isLowConfidence(1.0f)
        )
    }

    @Test
    fun `PT-U06j — confianza de 1,0 se guarda en historial`() {
        assertTrue(
            "1.0f debe guardarse en historial",
            ConfidenceUtils.shouldSaveToHistory(1.0f)
        )
    }

    // ── PT-U06k — Valores cercanos al umbral ──

    @Test
    fun `PT-U06k — confianza de 0,699 es baja confianza`() {
        assertTrue(
            "0.699f está por debajo del umbral",
            ConfidenceUtils.isLowConfidence(0.699f)
        )
    }

    @Test
    fun `PT-U06l — confianza de 0,701 no es baja confianza`() {
        assertFalse(
            "0.701f está por encima del umbral",
            ConfidenceUtils.isLowConfidence(0.701f)
        )
    }
}
