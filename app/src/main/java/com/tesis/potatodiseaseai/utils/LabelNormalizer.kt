package com.tesis.potatodiseaseai.utils

object LabelNormalizer {
    
    /**
     * Normaliza un label de enfermedad
     * Ejemplo: "Potato___Early_Blight" → "early blight"
     */
    fun normalize(label: String): String {
        return label
            .substringAfter("Potato___")  // Remover prefijo
            .replace("_", " ")             // Reemplazar guiones bajos
            .lowercase()                   // Convertir a minúsculas
            .trim()                        // Eliminar espacios
    }
}