package com.tesis.potatodiseaseai.utils

object ConfidenceUtils {
    const val CONFIDENCE_THRESHOLD = 0.70f

    fun isLowConfidence(confidence: Float): Boolean {
        return confidence < CONFIDENCE_THRESHOLD
    }

    fun shouldSaveToHistory(confidence: Float): Boolean {
        return !isLowConfidence(confidence)
    }
}
