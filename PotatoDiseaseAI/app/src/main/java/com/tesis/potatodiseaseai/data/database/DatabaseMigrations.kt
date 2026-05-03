package com.tesis.potatodiseaseai.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    /**
     * Migración v1 → v2.
     * Descarta la tabla 'detections' (historial no conservado por decisión de diseño)
     * y crea el nuevo esquema relacional: enfermedades + analisis.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `detections`")

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `enfermedades` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `labelCnn` TEXT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `agenteCausal` TEXT NOT NULL,
                    `impacto` TEXT NOT NULL,
                    `manifestacionesVisuales` TEXT NOT NULL,
                    `signosClave` TEXT NOT NULL,
                    `recomendaciones` TEXT NOT NULL,
                    `imagenReferencia` TEXT NOT NULL,
                    `imagenGradcam` TEXT NOT NULL,
                    `fuentes` TEXT NOT NULL
                )"""
            )

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `analisis` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `enfermedadId` INTEGER NOT NULL,
                    `imagenCapturada` TEXT NOT NULL,
                    `imagenGradcamReal` TEXT,
                    `precision` REAL NOT NULL,
                    `fechaHora` INTEGER NOT NULL,
                    FOREIGN KEY(`enfermedadId`) REFERENCES `enfermedades`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )"""
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_analisis_enfermedadId` ON `analisis` (`enfermedadId`)"
            )

            insertSeedDataV2(db)
        }
    }

    /**
     * Migración v2 → v3.
     * Actualiza los campos imagenReferencia e imagenGradcam con los nombres
     * de los drawables reales que fueron agregados al proyecto.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val imageMap = mapOf(
                "late blight"    to Pair("lateblight_normal", "lateblight_gradcam"),
                "early blight"   to Pair("earlyblight_normal", "earlyblight_gradcam"),
                "leafroll virus" to Pair("leafroll_normal", "leafroll_gradcam"),
                "mosaic virus"   to Pair("mosaic_normal", "mosaic_gradcam"),
                "bacterial wilt" to Pair("", ""),
                "nematode"       to Pair("nematode_normal", "nematode_gradacam"),
                "pest"           to Pair("pest_normal", "pest_gradcam"),
                "healthy"        to Pair("healthy_normal", "")
            )
            for ((label, images) in imageMap) {
                db.execSQL(
                    "UPDATE `enfermedades` SET imagenReferencia = ?, imagenGradcam = ? WHERE labelCnn = ?",
                    arrayOf(images.first, images.second, label)
                )
            }
        }
    }

    /**
     * Migración v3 → v4.
     * Cambios sustanciales:
     *  1. Tabla enfermedades: se eliminan manifestacionesVisuales, signosClave, recomendaciones
     *     y se agregan tipoAgente, patronVisual, prevencion, controlQuimico, controlBiologico.
     *  2. Tabla analisis: se elimina la columna imagenGradcamReal.
     *
     * Ambas tablas se recrean con el nuevo esquema (SQLite no soporta DROP COLUMN
     * en todas las versiones, por lo que se usa la estrategia de recreación).
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ══════════════════════════════════════════════════════
            // PASO 1: Recrear tabla ENFERMEDADES con nuevos campos
            // ══════════════════════════════════════════════════════
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `enfermedades_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `labelCnn` TEXT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `agenteCausal` TEXT NOT NULL,
                    `tipoAgente` TEXT NOT NULL DEFAULT '',
                    `patronVisual` TEXT NOT NULL DEFAULT '',
                    `impacto` TEXT NOT NULL,
                    `prevencion` TEXT NOT NULL DEFAULT '',
                    `controlQuimico` TEXT NOT NULL DEFAULT '',
                    `controlBiologico` TEXT NOT NULL DEFAULT '',
                    `imagenReferencia` TEXT NOT NULL,
                    `imagenGradcam` TEXT NOT NULL,
                    `fuentes` TEXT NOT NULL
                )"""
            )

            // Migrar datos existentes (campos obsoletos se descartan, nuevos quedan vacíos)
            db.execSQL(
                """INSERT INTO `enfermedades_new`
                   (id, labelCnn, nombre, agenteCausal, impacto,
                    imagenReferencia, imagenGradcam, fuentes)
                   SELECT id, labelCnn, nombre, agenteCausal, impacto,
                          imagenReferencia, imagenGradcam, fuentes
                   FROM `enfermedades`"""
            )

            db.execSQL("DROP TABLE `enfermedades`")
            db.execSQL("ALTER TABLE `enfermedades_new` RENAME TO `enfermedades`")

            // Actualizar con datos verificados por reportes
            updateVerifiedDiseaseData(db)

            // ══════════════════════════════════════════════════════
            // PASO 2: Recrear tabla ANALISIS sin imagenGradcamReal
            // ══════════════════════════════════════════════════════
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `analisis_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `enfermedadId` INTEGER NOT NULL,
                    `imagenCapturada` TEXT NOT NULL,
                    `precision` REAL NOT NULL,
                    `fechaHora` INTEGER NOT NULL,
                    FOREIGN KEY(`enfermedadId`) REFERENCES `enfermedades`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )"""
            )

            db.execSQL(
                """INSERT INTO `analisis_new`
                   (id, enfermedadId, imagenCapturada, `precision`, fechaHora)
                   SELECT id, enfermedadId, imagenCapturada, `precision`, fechaHora
                   FROM `analisis`"""
            )

            db.execSQL("DROP TABLE `analisis`")
            db.execSQL("ALTER TABLE `analisis_new` RENAME TO `analisis`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_analisis_enfermedadId` ON `analisis` (`enfermedadId`)"
            )
        }
    }

    /**
     * Actualiza los registros de enfermedades con la información verificada
     * por reportes académicos (v4).
     */
    private fun updateVerifiedDiseaseData(db: SupportSQLiteDatabase) {
        for (row in buildVerifiedUpdates()) {
            db.execSQL(
                """UPDATE `enfermedades` SET
                    agenteCausal = ?, tipoAgente = ?, patronVisual = ?,
                    impacto = ?, prevencion = ?, controlQuimico = ?,
                    controlBiologico = ?, fuentes = ?
                   WHERE labelCnn = ?""",
                row
            )
        }
    }

    /**
     * Cada array: [agenteCausal, tipoAgente, patronVisual, impacto,
     *              prevencion, controlQuimico, controlBiologico, fuentes, labelCnn]
     */
    private fun buildVerifiedUpdates(): List<Array<Any>> = listOf(
        // Tizón Tardío (E-01)
        arrayOf(
            "Phytophthora infestans",
            "Oomiceto",
            "Lesiones necróticas irregulares de aspecto aceitoso (marrón a negro), con halo verde pálido; en alta humedad aparece eflorescencia blanquecina en el envés de la hoja.",
            "La enfermedad más devastadora de la papa. Pérdidas de hasta el 100% si no se controla.",
            "Uso de variedades resistentes|Eliminación de rastrojos|Buen drenaje del terreno|Aporque alto|Corte del follaje antes de la cosecha",
            "Fungicidas preventivos: Mancozeb, Clorotalonil|Fungicidas sistémicos: Metalaxyl, Azoxystrobin",
            "Trichoderma spp.|Bacillus spp.|Pseudomonas spp.|Extractos vegetales (orégano)",
            "Reportes verificados — Tesis Vichicela 2026",
            "late blight"
        ),
        // Tizón Temprano (E-03)
        arrayOf(
            "Alternaria solani",
            "Hongo",
            "Manchas necróticas circulares con anillos concéntricos (tipo diana) rodeadas de clorosis.",
            "Pérdidas del 20% al 50%. Afecta principalmente plantas con estrés o déficit nutricional.",
            "Eliminar restos vegetales|Rotar cultivos con especies no solanáceas|Nutrición balanceada evitando estrés hídrico",
            "Clorotalonil|Mancozeb|Azoxystrobin",
            "Trichoderma longibrachiatum|Bacillus subtilis",
            "Reportes verificados — Tesis Vichicela 2026",
            "early blight"
        ),
        // Saludable
        arrayOf(
            "N/A — Planta sin patología detectada",
            "N/A",
            "Lámina foliar completamente verde, superficie lisa, plana y turgente. Sin manchas, lesiones ni deformaciones.",
            "Clase de referencia del sistema. Sin pérdidas asociadas.",
            "Buena preparación del suelo|Fertilización equilibrada (N, P, K, S)|Uso de semilla certificada|Riego adecuado|Desinfección de herramientas",
            "No requiere control químico",
            "No requiere control biológico",
            "Reportes verificados — Tesis Vichicela 2026",
            "healthy"
        ),
        // Plagas (E-17)
        arrayOf(
            "Tecia solanivora, Liriomyza huidobrensis, Epitrix spp.",
            "Insectos",
            "Galerías serpenteantes, perforaciones circulares y presencia de deyecciones oscuras en hojas y tubérculos.",
            "La polilla puede causar pérdida total del tubérculo; la mosca minadora reduce la capacidad fotosintética hasta un 60%.",
            "Aporques oportunos|Rotación de cultivos|Cosecha temprana|Uso de trampas (feromonas, cromáticas)",
            "Clorpirifos|Imidacloprid",
            "Nematodos entomopatógenos (Steinernema, Heterorhabditis)|Beauveria bassiana|Baculovirus",
            "Reportes verificados — Tesis Vichicela 2026",
            "pest"
        ),
        // Nematodo del Quiste (E-16)
        arrayOf(
            "Globodera pallida, G. rostochiensis",
            "Nematodo",
            "Enanismo generalizado y clorosis en parches dentro del cultivo. Síntomas indirectos en parte aérea.",
            "Reducción del rendimiento del 10% al 30%. Plaga cuarentenaria con alta incidencia en la sierra central.",
            "Rotaciones largas (hasta 7 años)|Uso de semilla certificada|Control del movimiento de suelo",
            "Nematicidas según recomendación técnica",
            "Uso de variedades con resistencia genética documentada",
            "Reportes verificados — Tesis Vichicela 2026",
            "nematode"
        ),
        // Virus del Enrollamiento (E-13)
        arrayOf(
            "Potato leafroll virus (PLRV)",
            "Virus",
            "Enrollamiento hacia arriba de los foliolos, textura coriácea y coloración pálida o rojiza.",
            "Pérdidas del 30% al 90%. Afecta severamente la calidad de la semilla.",
            "Uso de semilla certificada|Eliminación temprana de plantas infectadas",
            "Insecticidas contra el pulgón vector (imidacloprid)",
            "Aceites minerales que interfieren en la transmisión",
            "Reportes verificados — Tesis Vichicela 2026",
            "leafroll virus"
        ),
        // Virus del Mosaico (E-14)
        arrayOf(
            "Potato virus Y (PVY)",
            "Virus",
            "Mosaico rugoso con parches verde claro y oscuro, además de deformación foliar.",
            "Pérdidas del 20% al 80%. PVY por áfidos.",
            "Eliminar plantas enfermas|Evitar propagación cercana|Desinfectar herramientas",
            "Insecticidas contra pulgones (eficacia limitada)",
            "Aceites minerales para reducir la transmisión",
            "Reportes verificados — Tesis Vichicela 2026",
            "mosaic virus"
        ),
        // Marchitez Bacteriana
        arrayOf(
            "Ralstonia solanacearum",
            "Bacteria",
            "Amarillamiento y marchitez foliar uniforme sin manchas discretas. Hojas colgantes y flácidas.",
            "Puede causar pérdida total en suelos contaminados. Sobrevive años en el suelo.",
            "Rotación de cultivos con gramíneas por al menos 3 años|Eliminar y quemar plantas infectadas|Uso de semilla certificada",
            "No tiene cura efectiva — manejo exclusivamente preventivo",
            "No se dispone de agentes biológicos efectivos documentados",
            "Reportes verificados — Tesis Vichicela 2026",
            "bacterial wilt"
        )
    )

    /**
     * Inserta las 8 clases del modelo CNN como datos semilla (esquema v4).
     * Llamado desde el Callback onCreate (instalaciones nuevas).
     */
    fun insertSeedData(db: SupportSQLiteDatabase) {
        for (row in buildSeedRows()) {
            db.execSQL(
                """INSERT OR IGNORE INTO `enfermedades`
                   (labelCnn, nombre, agenteCausal, tipoAgente, patronVisual,
                    impacto, prevencion, controlQuimico, controlBiologico,
                    imagenReferencia, imagenGradcam, fuentes)
                   VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""",
                row
            )
        }
    }

    // Cada array: [labelCnn, nombre, agenteCausal, tipoAgente, patronVisual,
    //              impacto, prevencion, controlQuimico, controlBiologico,
    //              imagenReferencia, imagenGradcam, fuentes]
    private fun buildSeedRows(): List<Array<Any>> = listOf(
        arrayOf(
            "late blight",
            "Tizón Tardío o Lancha",
            "Phytophthora infestans",
            "Oomiceto",
            "Lesiones necróticas irregulares de aspecto aceitoso (marrón a negro), con halo verde pálido; en alta humedad aparece eflorescencia blanquecina en el envés de la hoja.",
            "La enfermedad más devastadora de la papa. Pérdidas de hasta el 100% si no se controla.",
            "Uso de variedades resistentes|Eliminación de rastrojos|Buen drenaje del terreno|Aporque alto|Corte del follaje antes de la cosecha",
            "Fungicidas preventivos: Mancozeb, Clorotalonil|Fungicidas sistémicos: Metalaxyl, Azoxystrobin",
            "Trichoderma spp.|Bacillus spp.|Pseudomonas spp.|Extractos vegetales (orégano)",
            "lateblight_normal", "lateblight_gradcam",
            "Reportes verificados — Tesis Vichicela 2026"
        ),
        arrayOf(
            "early blight",
            "Tizón Temprano",
            "Alternaria solani",
            "Hongo",
            "Manchas necróticas circulares con anillos concéntricos (tipo diana) rodeadas de clorosis.",
            "Pérdidas del 20% al 50%. Afecta principalmente plantas con estrés o déficit nutricional.",
            "Eliminar restos vegetales|Rotar cultivos con especies no solanáceas|Nutrición balanceada evitando estrés hídrico",
            "Clorotalonil|Mancozeb|Azoxystrobin",
            "Trichoderma longibrachiatum|Bacillus subtilis",
            "earlyblight_normal", "earlyblight_gradcam",
            "Reportes verificados — Tesis Vichicela 2026"
        ),
        arrayOf(
            "leafroll virus",
            "Virus del Enrollamiento de la Hoja (PLRV)",
            "Potato leafroll virus (PLRV)",
            "Virus",
            "Enrollamiento hacia arriba de los foliolos, textura coriácea y coloración pálida o rojiza.",
            "Pérdidas del 30% al 90%. Afecta severamente la calidad de la semilla.",
            "Uso de semilla certificada|Eliminación temprana de plantas infectadas",
            "Insecticidas contra el pulgón vector (imidacloprid)",
            "Aceites minerales que interfieren en la transmisión",
            "leafroll_normal", "leafroll_gradcam",
            "Reportes verificados — Tesis Vichicela 2026"
        ),
        arrayOf(
            "mosaic virus",
            "Mosaico Viral (PVY/PVX)",
            "Potato virus Y (PVY)",
            "Virus",
            "Mosaico rugoso con parches verde claro y oscuro, además de deformación foliar.",
            "Pérdidas del 20% al 80%. PVY por áfidos.",
            "Eliminar plantas enfermas|Evitar propagación cercana|Desinfectar herramientas",
            "Insecticidas contra pulgones (eficacia limitada)",
            "Aceites minerales para reducir la transmisión",
            "mosaic_normal", "mosaic_gradcam",
            "Reportes verificados — Tesis Vichicela 2026"
        ),
        arrayOf(
            "bacterial wilt",
            "Marchitez Bacteriana",
            "Ralstonia solanacearum",
            "Bacteria",
            "Amarillamiento y marchitez foliar uniforme sin manchas discretas. Hojas colgantes y flácidas.",
            "Puede causar pérdida total en suelos contaminados. Sobrevive años en el suelo.",
            "Rotación de cultivos con gramíneas por al menos 3 años|Eliminar y quemar plantas infectadas|Uso de semilla certificada",
            "No tiene cura efectiva — manejo exclusivamente preventivo",
            "No se dispone de agentes biológicos efectivos documentados",
            "", "",
            "Reportes verificados — Tesis Vichicela 2026"
        ),
        arrayOf(
            "nematode",
            "Nematodo del Quiste de la Papa (NQP)",
            "Globodera pallida, G. rostochiensis",
            "Nematodo",
            "Enanismo generalizado y clorosis en parches dentro del cultivo. Síntomas indirectos en parte aérea.",
            "Reducción del rendimiento del 10% al 30%. Plaga cuarentenaria con alta incidencia en la sierra central.",
            "Rotaciones largas (hasta 7 años)|Uso de semilla certificada|Control del movimiento de suelo",
            "Nematicidas según recomendación técnica",
            "Uso de variedades con resistencia genética documentada",
            "nematode_normal", "nematode_gradacam",
            "Reportes verificados — Tesis Vichicela 2026"
        ),
        arrayOf(
            "pest",
            "Daño por Plagas",
            "Tecia solanivora, Liriomyza huidobrensis, Epitrix spp.",
            "Insectos",
            "Galerías serpenteantes, perforaciones circulares y presencia de deyecciones oscuras en hojas y tubérculos.",
            "La polilla puede causar pérdida total del tubérculo; la mosca minadora reduce la capacidad fotosintética hasta un 60%.",
            "Aporques oportunos|Rotación de cultivos|Cosecha temprana|Uso de trampas (feromonas, cromáticas)",
            "Clorpirifos|Imidacloprid",
            "Nematodos entomopatógenos (Steinernema, Heterorhabditis)|Beauveria bassiana|Baculovirus",
            "pest_normal", "pest_gradcam",
            "Reportes verificados — Tesis Vichicela 2026"
        ),
        arrayOf(
            "healthy",
            "Planta Sana",
            "N/A — Planta sin patología detectada",
            "N/A",
            "Lámina foliar completamente verde, superficie lisa, plana y turgente. Sin manchas, lesiones ni deformaciones.",
            "Clase de referencia del sistema. Sin pérdidas asociadas.",
            "Buena preparación del suelo|Fertilización equilibrada (N, P, K, S)|Uso de semilla certificada|Riego adecuado|Desinfección de herramientas",
            "No requiere control químico",
            "No requiere control biológico",
            "healthy_normal", "",
            "Reportes verificados — Tesis Vichicela 2026"
        )
    )

    /**
     * Datos semilla originales del esquema v2.
     * Solo utilizado por MIGRATION_1_2 para usuarios que migran desde v1.
     */
    private fun insertSeedDataV2(db: SupportSQLiteDatabase) {
        val rows = listOf(
            arrayOf("late blight", "Tizón Tardío o Lancha", "Oomiceto Phytophthora infestans",
                "Pérdidas de hasta el 100% si no se controla.",
                "Manchas irregulares de aspecto húmedo y acuoso.", "Felpilla blanca en envés.",
                "Usar variedades resistentes|Aplicar fungicidas preventivos|Eliminar plantas voluntarias",
                "", "", "INIAP Ecuador"),
            arrayOf("early blight", "Tizón Temprano", "Hongo Alternaria solani",
                "Pérdidas del 20% al 50%.",
                "Manchas circulares con anillos concéntricos.", "Halo clorótico amarillo.",
                "Nutrición balanceada|Rotación de cultivos|Aplicar fungicidas",
                "", "", "INIAP Ecuador"),
            arrayOf("leafroll virus", "Virus del Enrollamiento de la Hoja (PLRV)",
                "Potato leafroll virus", "Pérdidas del 30% al 90%.",
                "Enrollamiento hacia arriba de los folíolos.", "Hojas con textura rígida.",
                "Control de áfidos|Uso de semilla certificada",
                "", "", "INIAP Ecuador"),
            arrayOf("mosaic virus", "Mosaico Viral (PVY/PVX)", "Potato virus Y (PVY)",
                "Pérdidas del 20% al 80%.", "Patrón mosaico.", "Superficie rugosa.",
                "Desinfección de herramientas|Eliminar plantas enfermas|Controlar áfidos",
                "", "", "INIAP Ecuador"),
            arrayOf("bacterial wilt", "Marchitez Bacteriana", "Bacteria Ralstonia solanacearum",
                "Pérdida total en suelos contaminados.", "Marchitez foliar uniforme.", "Marchitez diurna.",
                "Manejo preventivo|Rotación de cultivos|Eliminar plantas infectadas",
                "", "", "INIAP Ecuador"),
            arrayOf("nematode", "Nematodo del Quiste de la Papa (NQP)", "Globodera pallida",
                "Reducción del 10% al 30%.", "Clorosis difusa.", "Plantas pequeñas en rodales.",
                "Análisis de suelo|Rotaciones largas",
                "", "", "INIAP Ecuador"),
            arrayOf("pest", "Daño por Plagas",
                "Mosca minadora, pulguilla y polilla de papa",
                "Pérdida total del tubérculo posible.", "Destrucción mecánica del tejido foliar.",
                "Aspecto apolillado.",
                "Trampas de feromonas|Control biológico con Beauveria bassiana",
                "", "", "INIAP Ecuador"),
            arrayOf("healthy", "Planta Sana", "N/A", "Sin pérdidas.",
                "Lámina foliar completamente verde.", "Color verde uniforme.",
                "Monitoreo quincenal|Nutrición balanceada",
                "", "", "INIAP Ecuador")
        )
        for (row in rows) {
            db.execSQL(
                """INSERT OR IGNORE INTO `enfermedades`
                   (labelCnn, nombre, agenteCausal, impacto, manifestacionesVisuales,
                    signosClave, recomendaciones, imagenReferencia, imagenGradcam, fuentes)
                   VALUES (?,?,?,?,?,?,?,?,?,?)""",
                row
            )
        }
    }
}
