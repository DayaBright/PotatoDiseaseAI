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
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP TABLE IF EXISTS `detections`")

            database.execSQL(
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

            database.execSQL(
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

            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_analisis_enfermedadId` ON `analisis` (`enfermedadId`)"
            )

            insertSeedData(database)
        }
    }

    /**
     * Migración v2 → v3.
     * Actualiza los campos imagenReferencia e imagenGradcam con los nombres
     * de los drawables reales que fueron agregados al proyecto.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
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
                database.execSQL(
                    "UPDATE `enfermedades` SET imagenReferencia = ?, imagenGradcam = ? WHERE labelCnn = ?",
                    arrayOf(images.first, images.second, label)
                )
            }
        }
    }

    /**
     * Inserta las 8 clases del modelo CNN como datos semilla.
     * Llamado desde MIGRATION_1_2 (usuarios existentes) y desde
     * el Callback onCreate (instalaciones nuevas).
     */
    fun insertSeedData(db: SupportSQLiteDatabase) {
        for (row in buildSeedRows()) {
            db.execSQL(
                """INSERT OR IGNORE INTO `enfermedades`
                   (labelCnn, nombre, agenteCausal, impacto, manifestacionesVisuales,
                    signosClave, recomendaciones, imagenReferencia, imagenGradcam, fuentes)
                   VALUES (?,?,?,?,?,?,?,?,?,?)""",
                row
            )
        }
    }

    // Cada array: [labelCnn, nombre, agenteCausal, impacto, manifestacionesVisuales,
    //              signosClave, recomendaciones, imagenReferencia, imagenGradcam, fuentes]
    private fun buildSeedRows(): List<Array<Any>> = listOf(
        arrayOf(
            "late blight",
            "Tizón Tardío o Lancha",
            "Oomiceto Phytophthora infestans",
            "La enfermedad más devastadora en Ecuador, con pérdidas de hasta el 100% si no se controla. Aumenta críticos los costos de producción por uso intensivo de fungicidas.",
            "Manchas irregulares de aspecto húmedo y acuoso, bordes difusos (sin anillos) y color marrón-negro.",
            "En condiciones de alta humedad aparece felpilla blanca (micelio) en el envés de la hoja.",
            "Usar variedades resistentes del catálogo INIAP|Aplicar fungicidas preventivos (mancozeb) y curativos (metalaxil)|Eliminar plantas voluntarias (guachas) que son focos de infección",
            "lateblight_normal", "lateblight_gradcam", "INIAP Ecuador; Agrios G.N. Plant Pathology 5th ed."
        ),
        arrayOf(
            "early blight",
            "Tizón Temprano",
            "Hongo Alternaria solani",
            "Pérdidas del 20% al 50%. Afecta principalmente plantas con estrés o déficit nutricional.",
            "Manchas circulares o angulares con anillos concéntricos (patrón de diana u ojo de buey) y textura seca.",
            "Manchas rodeadas por halo clorótico amarillo; se inician en hojas basales (más viejas).",
            "Nutrición balanceada con nitrógeno para fortalecer la planta|Rotación de cultivos con especies no solanáceas|Aplicar fungicidas (azoxistrobina, boscalid) al inicio de floración",
            "earlyblight_normal", "earlyblight_gradcam", "INIAP Ecuador; Agrios G.N. Plant Pathology 5th ed."
        ),
        arrayOf(
            "leafroll virus",
            "Virus del Enrollamiento de la Hoja (PLRV)",
            "Potato leafroll virus — transmitido por áfidos/pulgones",
            "Pérdidas del 30% al 90%. Afecta severamente la calidad de la semilla.",
            "Enrollamiento hacia arriba de los folíolos dándoles forma de tubo o canaleta.",
            "Hojas con textura rígida y coriácea (crujiente al tacto) y coloración rojiza o púrpura en los márgenes.",
            "Control estricto de áfidos vectores con insecticidas sistémicos (imidacloprid)|Uso exclusivo de semilla certificada libre de virus",
            "leafroll_normal", "leafroll_gradcam", "INIAP Ecuador; Salazar L.F. Potato Viruses and their Management"
        ),
        arrayOf(
            "mosaic virus",
            "Mosaico Viral (PVY/PVX)",
            "Potato virus Y (PVY) y Potato virus X (PVX)",
            "Pérdidas del 20% al 80%. PVX por contacto mecánico; PVY por áfidos.",
            "Patrón mosaico (áreas verde oscuro, verde claro y amarillo intercaladas) en lámina foliar relativamente plana.",
            "Superficie rugosa o ampollada (con bultos) y deformación de los bordes foliares.",
            "Desinfección de herramientas con hipoclorito al 1% para evitar transmisión mecánica|Eliminar plantas enfermas del campo|Controlar áfidos vectores",
            "mosaic_normal", "mosaic_gradcam", "INIAP Ecuador; Salazar L.F. Potato Viruses and their Management"
        ),
        arrayOf(
            "bacterial wilt",
            "Marchitez Bacteriana",
            "Bacteria Ralstonia solanacearum",
            "Puede causar pérdida total en suelos contaminados. Sobrevive años en el suelo y no tiene cura efectiva.",
            "Amarillamiento y marchitez foliar uniforme sin manchas discretas. Hojas colgantes y flácidas.",
            "Marchitez diurna (la planta se desmaya con el sol) con recuperación parcial durante la noche en estadios iniciales.",
            "No tiene cura efectiva — el manejo es preventivo|Rotación de cultivos con gramíneas por al menos 3 años|Eliminar y quemar plantas infectadas (nunca compostar)",
            "", "", "INIAP Ecuador; Hayward A.C. (1991) Biology and epidemiology of bacterial wilt"
        ),
        arrayOf(
            "nematode",
            "Nematodo del Quiste de la Papa (NQP)",
            "Globodera pallida",
            "Reducción del rendimiento del 10% al 30%. Plaga cuarentenaria con alta incidencia en la sierra central (Cotopaxi).",
            "Clorosis difusa generalizada y enanismo de la planta.",
            "Plantas pequeñas agrupadas en rodales o manchones irregulares dentro del campo.",
            "Análisis de suelo antes de la siembra para detectar presencia del nematodo|Rotaciones largas y uso de variedades con tolerancia documentada por INIAP",
            "nematode_normal", "nematode_gradacam", "INIAP Ecuador; CIP (International Potato Center)"
        ),
        arrayOf(
            "pest",
            "Daño por Plagas",
            "Mosca minadora (Liriomyza huidobrensis), pulguilla (Epitrix spp.) y polilla de papa (Tecia solanivora)",
            "La polilla puede causar pérdida total del tubérculo; la mosca minadora reduce la capacidad fotosintética hasta un 60%.",
            "Destrucción mecánica irregular del tejido foliar: huecos, perforaciones o minas serpenteadas.",
            "Aspecto apolillado de la hoja. La mosca minadora deja túneles blancos-amarillentos visibles al trasluz.",
            "Uso de trampas de feromonas para polillas y trampas amarillas para mosca minadora|Control biológico con hongos entomopatógenos como Beauveria bassiana",
            "pest_normal", "pest_gradcam", "INIAP Ecuador; SENASA Perú"
        ),
        arrayOf(
            "healthy",
            "Planta Sana",
            "N/A — Planta sin patología detectada",
            "Clase de referencia del sistema. Sin pérdidas asociadas.",
            "Lámina foliar completamente verde, superficie lisa, plana y turgente.",
            "Color verde uniforme sin manchas, lesiones, deformaciones ni signos de patógenos.",
            "Programa preventivo de monitoreo quincenal|Nutrición balanceada N-P-K según análisis de suelo",
            "healthy_normal", "", "INIAP Ecuador"
        )
    )
}
