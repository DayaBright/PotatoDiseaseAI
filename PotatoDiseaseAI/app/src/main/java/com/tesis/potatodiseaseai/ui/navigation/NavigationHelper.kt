package com.tesis.potatodiseaseai.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NavigationHelper {
    
    /**
     * Codifica una URI para navegación segura
     */
    fun encodeUri(uri: String): String {
        return URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
    }
    
    /**
     * Decodifica una URI desde parámetros de navegación
     */
    fun decodeUri(encodedUri: String): String {
        return URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
    }
    
    /**
     * Construye la ruta completa para ResultScreen
     */
    fun buildResultRoute(
        imageUri: String,
        disease: String,
        confidence: Float,
        detectionId: Long
    ): String {
        val encodedUri = encodeUri(imageUri)
        val encodedDisease = encodeUri(disease)
        return "result/$encodedUri/$encodedDisease/$confidence/$detectionId"
    }
    
    /**
     * Rutas de la aplicación
     */
    object Routes {
        const val ONBOARDING = "onboarding"
        const val SCANNER = "scanner"
        const val HISTORY = "history"
        const val HELP = "help"
        const val DISEASE_DETAIL_BASE = "disease_detail"
        const val DISEASE_DETAIL_FULL = "disease_detail/{enfermedadId}"
        const val RESULT_BASE = "result"
        const val RESULT_FULL = "result/{imageUri}/{disease}/{confidence}/{detectionId}"
    }
    
    /**
     * Argumentos de navegación
     */
    object Args {
        const val IMAGE_URI = "imageUri"
        const val DISEASE = "disease"
        const val CONFIDENCE = "confidence"
        const val DETECTION_ID = "detectionId"
        const val ENFERMEDAD_ID = "enfermedadId"
    }

    fun buildDiseaseDetailRoute(enfermedadId: Long): String {
        return "disease_detail/$enfermedadId"
    }
}