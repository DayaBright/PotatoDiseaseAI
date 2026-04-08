package com.tesis.potatodiseaseai.data.model

import android.net.Uri
import com.tesis.potatodiseaseai.utils.LabelNormalizer

data class DetectionResult(
    val imageUri: Uri,
    val disease: String,
    val confidence: Float,
    val gradcamUri: Uri? = null,
    val recommendations: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

object DiseaseDatabase {
    private val diseaseInfo = mapOf(
        "bacterial wilt" to DiseaseInfo(
            name = "Marchitez bacteriana",
            recommendations = listOf(
                "Eliminar plantas infectadas inmediatamente",
                "Rotar cultivos durante 3-4 años",
                "Usar variedades resistentes",
                "Desinfectar herramientas de trabajo"
            )
        ),
        "early blight" to DiseaseInfo(
            name = "Tizón temprano",
            recommendations = listOf(
                "Aplicar fungicidas a base de cobre",
                "Eliminar hojas afectadas",
                "Mejorar ventilación entre plantas",
                "Evitar riego por aspersión"
            )
        ),
        "healthy" to DiseaseInfo(
            name = "Planta sana",
            recommendations = listOf(
                "Mantener prácticas de cultivo actuales",
                "Monitorear regularmente",
                "Asegurar nutrición adecuada",
                "Continuar con riego apropiado"
            )
        ),
        "late blight" to DiseaseInfo(
            name = "Tizón tardío",
            recommendations = listOf(
                "Aplicar fungicidas preventivos",
                "Eliminar plantas severamente infectadas",
                "Evitar humedad excesiva",
                "Cosechar tubérculos antes de que se infecten"
            )
        ),
        "leafroll virus" to DiseaseInfo(
            name = "Virus del enrollamiento",
            recommendations = listOf(
                "Usar semilla certificada",
                "Controlar pulgones vectores",
                "Eliminar plantas infectadas",
                "No guardar tubérculos para semilla"
            )
        ),
        "mosaic virus" to DiseaseInfo(
            name = "Virus del mosaico",
            recommendations = listOf(
                "Plantar material certificado",
                "Controlar insectos vectores",
                "Eliminar plantas afectadas temprano",
                "Desinfectar herramientas"
            )
        ),
        "nematode" to DiseaseInfo(
            name = "Nematodos",
            recommendations = listOf(
                "Rotar con cultivos no hospederos",
                "Aplicar nematicidas biológicos",
                "Usar variedades resistentes",
                "Solarizar el suelo antes de plantar"
            )
        ),
        "pest" to DiseaseInfo(
            name = "Plagas",
            recommendations = listOf(
                "Identificar la plaga específica",
                "Usar control biológico cuando sea posible",
                "Aplicar insecticidas selectivos",
                "Monitorear regularmente el cultivo"
            )
        )
    )

    data class DiseaseInfo(
        val name: String,
        val recommendations: List<String>
    )

    fun getRecommendations(diseaseLabel: String): List<String> {
        // ✅ Usar normalización centralizada
        val normalizedLabel = LabelNormalizer.normalize(diseaseLabel)
        return diseaseInfo[normalizedLabel]?.recommendations ?: listOf(
            "Consultar con un agrónomo",
            "Tomar más fotos para análisis",
            "Verificar el diagnóstico"
        )
    }

    fun getDiseaseName(diseaseLabel: String): String {
        // ✅ Usar normalización centralizada
        val normalizedLabel = LabelNormalizer.normalize(diseaseLabel)
        return diseaseInfo[normalizedLabel]?.name ?: diseaseLabel
    }
}