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
     *  1. Tabla enfermedades: se eliminan manifestacionesVisuales, signosClave, recomendaciones
     *     y se agregan tipoAgente, patronVisual, prevencion, controlQuimico, controlBiologico.
     *  2. Tabla analisis: se elimina la columna imagenGradcamReal
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
     * Migración v4 → v5.
     * Agrega columnas para los detalles explicativos de tratamientos.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `enfermedades` ADD COLUMN `detalleControlQuimico` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `enfermedades` ADD COLUMN `detalleControlBiologico` TEXT NOT NULL DEFAULT ''")
            updateVerifiedDiseaseData(db)
        }
    }

    /**
     * Migración v5 → v6.
     * Actualiza el nombre de la patología 'mosaic virus' a 'Mosaico Viral (PVY)'.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "UPDATE `enfermedades` SET nombre = 'Mosaico Viral (PVY)' WHERE labelCnn = 'mosaic virus'"
            )
        }
    }

    /**
     * Actualiza los registros de enfermedades con la información verificada
     * por reportes académicos (v4 y v5).
     */
    private fun updateVerifiedDiseaseData(db: SupportSQLiteDatabase) {
        for (row in buildVerifiedUpdates()) {
            db.execSQL(
                """UPDATE `enfermedades` SET
                    agenteCausal = ?, tipoAgente = ?, patronVisual = ?,
                    impacto = ?, prevencion = ?, controlQuimico = ?,
                    controlBiologico = ?, detalleControlQuimico = ?,
                    detalleControlBiologico = ?, fuentes = ?
                   WHERE labelCnn = ?""",
                row
            )
        }
    }

    /**
     * Cada array: [agenteCausal, tipoAgente, patronVisual, impacto,
     *              prevencion, controlQuimico, controlBiologico, 
     *              detalleControlQuimico, detalleControlBiologico, fuentes, labelCnn]
     */
    private fun buildVerifiedUpdates(): List<Array<Any>> = listOf(
        // Tizón Tardío (E-01)
        arrayOf(
            "Phytophthora infestans",
            "Oomiceto",
            "Lesiones necróticas irregulares de aspecto aceitoso (marrón a negro), con halo verde pálido; en alta humedad aparece eflorescencia blanquecina en el envés de la hoja.",
            "La enfermedad más devastadora de la papa. Pérdidas de hasta el 100% si no se controla.",
            "Uso de variedades resistentes|Eliminación de rastrojos|Buen drenaje del terreno|Aporque alto|Corte del follaje antes de la cosecha",
            "Fungicidas sistémicos (antes de cerrar surco)|Fungicidas de contacto (al final del cultivo)|Máximo 3 aplicaciones sistémicas",
            "Trichoderma spp.|Bacillus spp.|Aplicación preventiva",
            "El control químico se realiza con fungicidas que se dividen en dos tipos: los 'sistémicos', que penetran dentro de la hoja y protegen a la planta por dentro durante unos 12 a 14 días, y los de 'contacto', que quedan por fuera como un escudo protector. Se recomienda empezar a fumigar a los 26 o 30 días de la siembra (antes de que las hojas cierren el surco), usando sistémicos como Ridomil Gold (usar de 250 a 500 gramos por cada 200 litros de agua) o Phyton (250 cm3 por 200 litros). Para que el hongo no se vuelva resistente, aplique los sistémicos máximo tres veces y luego termine el cultivo aplicando solo productos de contacto como Triziman D (500 gramos por 200 litros) o Daconil 720 (400 cm3 por 200 litros).",
            "Se aplican organismos vivos microscópicos 'buenos' como el hongo Trichoderma spp. o la bacteria Bacillus spp., los cuales actúan como enemigos naturales que se comen al hongo malo o no le dejan espacio para vivir. Estos deben aplicarse de forma preventiva, es decir, antes de que aparezca la enfermedad. El clima andino favorable para que este hongo ataque son las mañanas frías y lluviosas (temperaturas entre 10°C y 21°C), seguidas de tardes soleadas que ayudan a esparcir la enfermedad con el viento.",
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
            "Fungicidas sistémicos curativos|Fungicidas preventivos de contacto|Aplicar al notar primeras manchas",
            "Trichoderma harzianum|Bacillus subtilis|Aplicación preventiva antes de ataque severo",
            "El control químico no debe hacerse con fumigaciones continuas o exageradas, sino cuando se detecten las primeras manchas con anillos en las hojas viejas de abajo. Se utilizan fungicidas sistémicos curativos como Score 250 EC (100 cm3 por 200 litros de agua) o Quadris (100 g por 200 litros), o preventivos de contacto como Cuprofix 30 y Triziman D (500 g por 200 litros).",
            "El control biológico utiliza microorganismos beneficiosos como el hongo Trichoderma harzianum o la bacteria Bacillus subtilis; al igual que en la rancha, estos son microbios buenos que compiten por el alimento en la hoja, protegiendo a la planta, y deben rociarse como un escudo preventivo antes de que el ataque sea severo. Según las fuentes, su clima más favorable en la zona andina es más cálido que el de la rancha, prefiriendo temperaturas entre 24°C y 34°C, o días donde alternan lluvias con clima seco y caluroso.",
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
            "Su cultivo se encuentra en buenas condiciones. No se necesita ningún tipo de fumigación o veneno; mantenga el monitoreo rutinario del campo.",
            "Siga manteniendo sus prácticas habituales y el terreno nutrido para conservar la salud del cultivo y prevenir ataques.",
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
            "Insecticidas de amplio espectro|Lambda Cihalotrina o Clorpirifos|Alternar productos",
            "Aplicar Bacillus thuringiensis (Bt)|Extracto de Neem|Trampas amarillas pegajosas",
            "Apenas note en las hojas los primeros agujeros (pulguillas), caminos secos y transparentes (minadores y polillas) o mordeduras grandes (gusanos trozadores), puede pulverizar insecticidas de amplio espectro. Puede usar Lambda Cihalotrina (Ninja, en dosis de 100 a 500 cm3 por cada 200 litros de agua, dependiendo de qué tan fuerte sea el ataque), Clorpirifos (Puñete, a 250 cm3 por 200 litros), o Abamectina (Vertimec, a 100 cm3 por 200 litros). Es vital ir cambiando o alternando estos productos en cada aplicación para que las plagas no se vuelvan resistentes al veneno.",
            "Puede fumigar con un producto ecológico conocido como Bt (Bacillus thuringiensis variedad Kurstaki, a dosis de 250 gramos por 200 litros de agua). Esto no es un químico, sino una bacteria natural que al caer en la hoja es devorada por las crías de polilla y por los gusanos, enfermándolos del estómago hasta matarlos. También puede rociar extractos botánicos naturales como el Extracto de Neem (Neem X, usando 250 cm3 por 200 litros) que sirve para asfixiar y repeler pulgones y pulguillas de forma orgánica. Además, el uso de trampas amarillas pegajosas atrapa a los adultos voladores de todas estas plagas antes de que pongan huevos. Estas plagas tienden a multiplicarse violentamente en épocas de sequía.",
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
            "Nematicidas fuertes aplicados al suelo|Furadan 10G o 4F|Inyección de Metam sodio (opcional)",
            "Producto Intercept al suelo|Incorporar abundante guano/materia orgánica|Limpieza de herramientas",
            "El tratamiento químico se hace directamente a la tierra, ya que ahí vive la plaga. Se usan nematicidas fuertes como el Furadan 10G (a dosis de 30 kilos de producto seco por hectárea) o Furadan 4F (6 litros por hectárea líquidos). Estos se aplican al momento de depositar la semilla o a los 15-20 días de la siembra directo al pie de la plantita para cuidar la raíz. Otro químico usado es el Metam sodio, un gas que se inyecta en la tierra (entre 10°C y 25°C) y debe taparse con plástico por 15 días, aunque es un método muy costoso.",
            "Existe un producto llamado 'Intercept' que contiene bacterias naturales de raíz y se aplica al suelo a dosis de 2 a 3 cm3 por litro de agua. Además, incorporar mucha materia orgánica (guano) ayuda enormemente, ya que de ahí nacen hongos y bacterias buenas que atacan a los nematodos antes de que lleguen a la papa. El patógeno sobrevive años en la tierra fría adherida a herramientas o semillas, por lo que es vital limpiar todo.",
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
            "Insecticidas contra pulgones y trips|Imidacloprid o Thiamethoxam|Alternar productos",
            "Fomentar mariquitas y sarantontones|Avispas parasitoides (Aphidius colemani)|No abusar de químicos",
            "Ningún líquido químico puede curar a una planta que ya tiene el virus. El tratamiento químico se basa en aplicar insecticidas para matar a los pulgones y trips, que son los insectos (vectores) que contagian la enfermedad al picar la hoja sana. En cuanto vea focos de pulgones, debe aplicar insecticidas sistémicos (como Imidacloprid o Thiamethoxam) o de contacto (como Cipermetrina 25 a dosis de 100 cm3 por 200 litros), y es clave ir cambiando de producto para que el insecto no se haga inmune.",
            "El control biológico lo hace la propia naturaleza a través de insectos 'depredadores' (como las mariquitas o sarantontones que devoran al pulgón) o avispitas 'parasitoides' (como Aphidius colemani, la cual inyecta su huevo dentro del pulgón para que su cría se lo coma por dentro y lo mate). Por esto, se recomienda no abusar de químicos para no matar a estos aliados vivos. Estos insectos que transmiten el virus se multiplican muy rápido en climas andinos que se vuelven secos y calurosos por falta de lluvia.",
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
            "Insecticidas tempranos contra áfidos|Imidacloprid o Tiametoxam|No cura la planta enferma",
            "Dejar vivir mariquitas y crisopas|Control cultural (selección negativa)|Trampas amarillas con pegamento",
            "El tratamiento no cura la planta enferma, sino que se enfoca en eliminar a los pulgones o áfidos, que son los insectos que transmiten la enfermedad al picar las hojas. Químicamente, debe aplicar insecticidas apenas note la presencia de estos insectos. Puede usar productos sistémicos como el Imidacloprid (producto Agresor, aplicando 100 cm3 por 200 litros de agua) o Tiametoxam (Actara, de 100 a 150 gramos por 200 litros), o de contacto como Lambda Cihalotrina (Ninja, a 100 cm3 por 200 litros).",
            "Su mejor aliado es dejar vivir a los insectos 'buenos' de su parcela, como las mariquitas (sarantontones), las crisopas y unas pequeñas avispitas (como Aphidius colemani) que devoran a los pulgones o inyectan sus huevos en ellos para matarlos de forma natural. Sin embargo, la medida más importante es el control cultural (selección negativa): debe revisar su campo desde temprano y arrancar de raíz para quemar cualquier planta que nazca enana, arrugada o deforme, así evita que el virus se propague. Colocar franjas de plástico amarillo con pegamento alrededor del campo también ayuda a atrapar a los insectos voladores. Estos pulgones atacan con más fuerza en climas secos y calurosos donde hay falta de lluvia.",
            "Reportes verificados — Tesis Vichicela 2026",
            "mosaic virus"
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
                    detalleControlQuimico, detalleControlBiologico,
                    imagenReferencia, imagenGradcam, fuentes)
                   VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
                row
            )
        }
    }

    // Cada array: [labelCnn, nombre, agenteCausal, tipoAgente, patronVisual,
    //              impacto, prevencion, controlQuimico, controlBiologico,
    //              detalleControlQuimico, detalleControlBiologico,
    //              imagenReferencia, imagenGradcam, fuentes]
    private fun buildSeedRows(): List<Array<Any>> = listOf(
        buildVerifiedUpdates()[0].let { arrayOf(it[10], "Tizón Tardío o Lancha", it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], "lateblight_normal", "lateblight_gradcam", it[9]) },
        buildVerifiedUpdates()[1].let { arrayOf(it[10], "Tizón Temprano", it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], "earlyblight_normal", "earlyblight_gradcam", it[9]) },
        buildVerifiedUpdates()[5].let { arrayOf(it[10], "Virus del Enrollamiento de la Hoja (PLRV)", it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], "leafroll_normal", "leafroll_gradcam", it[9]) },
        buildVerifiedUpdates()[6].let { arrayOf(it[10], "Mosaico Viral (PVY)", it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], "mosaic_normal", "mosaic_gradcam", it[9]) },
        buildVerifiedUpdates()[4].let { arrayOf(it[10], "Nematodo del Quiste de la Papa (NQP)", it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], "nematode_normal", "nematode_gradacam", it[9]) },
        buildVerifiedUpdates()[3].let { arrayOf(it[10], "Daño por Plagas", it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], "pest_normal", "pest_gradcam", it[9]) },
        buildVerifiedUpdates()[2].let { arrayOf(it[10], "Planta Sana", it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], "healthy_normal", "", it[9]) }
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
            arrayOf("mosaic virus", "Mosaico Viral (PVY)", "Potato virus Y (PVY)",
                "Pérdidas del 20% al 80%.", "Patrón mosaico.", "Superficie rugosa.",
                "Desinfección de herramientas|Eliminar plantas enfermas|Controlar áfidos",
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
