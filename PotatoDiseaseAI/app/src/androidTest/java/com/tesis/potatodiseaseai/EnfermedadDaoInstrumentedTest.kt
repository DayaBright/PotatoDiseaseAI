package com.tesis.potatodiseaseai

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tesis.potatodiseaseai.data.database.AppDatabase
import com.tesis.potatodiseaseai.data.database.EnfermedadDao
import com.tesis.potatodiseaseai.data.database.EnfermedadEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ==========================================================================
 *  PT-I04 — Pruebas instrumentadas para EnfermedadDao
 *  Archivo: src/androidTest/.../EnfermedadDaoInstrumentedTest.kt
 *  Se ejecutan en un dispositivo o emulador Android.
 * ==========================================================================
 *
 *  RF cubierto: RF-14 (biblioteca de enfermedades)
 *  HU cubiertas: HU-09 (guía educativa), HU-03 (nombre enfermedad)
 *
 *  Casos cubiertos:
 *  - PT-I04a: Inserción y recuperación de enfermedades
 *  - PT-I04b: Búsqueda por etiqueta CNN (getByLabel)
 *  - PT-I04c: Conteo de enfermedades
 *  - PT-I04d: Búsqueda por ID
 *  - PT-I04e: Listas de prevención/control se descomponen correctamente
 *  - PT-I04f: OnConflictStrategy.IGNORE evita duplicados
 */
@RunWith(AndroidJUnit4::class)
class EnfermedadDaoInstrumentedTest {

    private lateinit var database: AppDatabase
    private lateinit var enfermedadDao: EnfermedadDao

    /** Conjunto de enfermedades semilla representativo (3 de 7) */
    private val enfermedadesSemilla = listOf(
        EnfermedadEntity(
            id = 1,
            labelCnn = "late blight",
            nombre = "Tizón tardío",
            agenteCausal = "Phytophthora infestans",
            tipoAgente = "Oomiceto",
            patronVisual = "Lesiones necróticas irregulares de aspecto aceitoso",
            impacto = "Pérdidas de hasta el 100% en condiciones favorables",
            prevencion = "Uso de variedades resistentes|Eliminación de rastrojos|Evitar riego por aspersión",
            controlQuimico = "Mancozeb|Metalaxyl|Clorotalonil",
            controlBiologico = "Trichoderma spp.|Bacillus spp.",
            imagenReferencia = "lateblight_normal",
            imagenGradcam = "lateblight_gradcam",
            fuentes = "CIP 2023"
        ),
        EnfermedadEntity(
            id = 2,
            labelCnn = "healthy",
            nombre = "Planta sana",
            agenteCausal = "N/A — Planta sin patología detectada",
            tipoAgente = "N/A",
            patronVisual = "Color verde uniforme, sin manchas ni deformaciones",
            impacto = "Clase de referencia — sin impacto negativo",
            prevencion = "Mantener prácticas de cultivo actuales|Monitoreo regular",
            controlQuimico = "No requiere tratamiento químico",
            controlBiologico = "No requiere control biológico",
            imagenReferencia = "healthy_normal",
            imagenGradcam = "healthy_gradcam",
            fuentes = "CIP 2023"
        ),
        EnfermedadEntity(
            id = 3,
            labelCnn = "early blight",
            nombre = "Tizón temprano",
            agenteCausal = "Alternaria solani",
            tipoAgente = "Hongo",
            patronVisual = "Manchas concéntricas con anillos en forma de diana",
            impacto = "Pérdidas del 20-50% en follaje",
            prevencion = "Rotación de cultivos|Eliminar hojas afectadas",
            controlQuimico = "Fungicidas a base de cobre|Clorotalonil",
            controlBiologico = "Trichoderma harzianum",
            imagenReferencia = "earlyblight_normal",
            imagenGradcam = "earlyblight_gradcam",
            fuentes = "CIP 2023"
        )
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        enfermedadDao = database.enfermedadDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── PT-I04a — Inserción y recuperación ──

    @Test
    fun insertarYRecuperarEnfermedades_listadoCompleto() = runTest {
        // Arrange & Act
        enfermedadDao.insertAll(enfermedadesSemilla)
        val todas = enfermedadDao.getAllEnfermedades().first()

        // Assert
        assertEquals(
            "Se deben recuperar 3 enfermedades",
            3, todas.size
        )
    }

    // ── PT-I04b — Búsqueda por etiqueta CNN ──

    @Test
    fun buscarPorLabel_devuelveEnfermedadCorrecta() = runTest {
        enfermedadDao.insertAll(enfermedadesSemilla)

        val resultado = enfermedadDao.getByLabel("late blight")

        assertNotNull("Debe encontrar 'late blight'", resultado)
        assertEquals("Tizón tardío", resultado!!.nombre)
        assertEquals("Phytophthora infestans", resultado.agenteCausal)
        assertEquals("Oomiceto", resultado.tipoAgente)
    }

    @Test
    fun buscarPorLabel_etiquetaInexistente_devuelveNull() = runTest {
        enfermedadDao.insertAll(enfermedadesSemilla)

        val resultado = enfermedadDao.getByLabel("inexistente")

        assertNull(
            "Una etiqueta inexistente debe devolver null",
            resultado
        )
    }

    // ── PT-I04c — Conteo ──

    @Test
    fun contarEnfermedades_devuelveCantidadCorrecta() = runTest {
        enfermedadDao.insertAll(enfermedadesSemilla)

        val count = enfermedadDao.count()

        assertEquals("Debe contar 3 enfermedades", 3, count)
    }

    @Test
    fun contarEnfermedades_tablaVacia_devuelveCero() = runTest {
        val count = enfermedadDao.count()
        assertEquals("Tabla vacía debe devolver 0", 0, count)
    }

    // ── PT-I04d — Búsqueda por ID ──

    @Test
    fun buscarPorId_devuelveEnfermedadCorrecta() = runTest {
        enfermedadDao.insertAll(enfermedadesSemilla)

        val resultado = enfermedadDao.getById(2L)

        assertNotNull("Debe encontrar la enfermedad con id=2", resultado)
        assertEquals("healthy", resultado!!.labelCnn)
        assertEquals("Planta sana", resultado.nombre)
    }

    @Test
    fun buscarPorId_idInexistente_devuelveNull() = runTest {
        enfermedadDao.insertAll(enfermedadesSemilla)

        val resultado = enfermedadDao.getById(999L)

        assertNull("Un ID inexistente debe devolver null", resultado)
    }

    // ── PT-I04e — Listas pipe-separated se descomponen correctamente ──

    @Test
    fun listasPipeSeparated_seDescomponenCorrectamente() = runTest {
        enfermedadDao.insertAll(enfermedadesSemilla)

        val tizon = enfermedadDao.getByLabel("late blight")!!

        val prevList = tizon.getPrevencionList()
        val quimList = tizon.getControlQuimicoList()
        val bioList = tizon.getControlBiologicoList()

        assertEquals("Prevención debe tener 3 ítems", 3, prevList.size)
        assertEquals("Control químico debe tener 3 ítems", 3, quimList.size)
        assertEquals("Control biológico debe tener 2 ítems", 2, bioList.size)

        assertTrue("Prevención contiene 'Uso de variedades resistentes'",
            prevList.contains("Uso de variedades resistentes"))
        assertTrue("Control químico contiene 'Mancozeb'",
            quimList.contains("Mancozeb"))
        assertTrue("Control biológico contiene 'Trichoderma spp.'",
            bioList.contains("Trichoderma spp."))
    }

    // ── PT-I04f — IGNORE evita duplicados ──

    @Test
    fun insertarDuplicados_seIgnoranSinError() = runTest {
        // Insertar una vez
        enfermedadDao.insertAll(enfermedadesSemilla)
        // Insertar de nuevo las mismas (OnConflictStrategy.IGNORE)
        enfermedadDao.insertAll(enfermedadesSemilla)

        val count = enfermedadDao.count()
        assertEquals(
            "No debe haber duplicados (IGNORE strategy)",
            3, count
        )
    }

    // ── PT-I04g — Orden alfabético ──

    @Test
    fun getAllEnfermedades_ordenAlfabetico() = runTest {
        enfermedadDao.insertAll(enfermedadesSemilla)

        val todas = enfermedadDao.getAllEnfermedades().first()

        // Verificar que están ordenadas por nombre ASC
        for (i in 0 until todas.size - 1) {
            assertTrue(
                "'${todas[i].nombre}' debe ser <= '${todas[i + 1].nombre}' (orden ASC)",
                todas[i].nombre <= todas[i + 1].nombre
            )
        }
    }
}
