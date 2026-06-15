package com.tesis.potatodiseaseai

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tesis.potatodiseaseai.data.database.AnalisisDao
import com.tesis.potatodiseaseai.data.database.AnalisisEntity
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
 *  PT-I05 — Pruebas de relación FK y CASCADE
 *  Se ejecutan en un dispositivo o emulador Android.
 * ==========================================================================
 *
 *  RF cubiertos: RF-11 (almacenar historial con FK a enfermedad),
 *                RF-08 (resultado con nombre de enfermedad asociado)
 *  HU cubiertas: HU-06 (historial con enfermedad relacionada)
 *  RNF cubierto: RNF-04 (persistencia con relaciones FK)
 *
 *  Casos cubiertos:
 *  - PT-I05a: AnalisisConEnfermedad resuelve la relación @Relation
 *  - PT-I05b: FK CASCADE elimina análisis al eliminar enfermedad
 *  - PT-I05c: Orden cronológico inverso (fechaHora DESC)
 *  - PT-I05d: Múltiples análisis para una misma enfermedad
 */
@RunWith(AndroidJUnit4::class)
class RelacionFkCascadeInstrumentedTest {

    private lateinit var database: AppDatabase
    private lateinit var analisisDao: AnalisisDao
    private lateinit var enfermedadDao: EnfermedadDao

    private val enfermedadLateBlight = EnfermedadEntity(
        id = 1,
        labelCnn = "late blight",
        nombre = "Tizón tardío",
        agenteCausal = "Phytophthora infestans",
        tipoAgente = "Oomiceto",
        patronVisual = "Lesiones necróticas",
        impacto = "Alto",
        prevencion = "Variedades resistentes",
        controlQuimico = "Mancozeb",
        controlBiologico = "Trichoderma spp.",
        imagenReferencia = "lateblight_normal",
        imagenGradcam = "lateblight_gradcam",
        fuentes = "CIP 2023"
    )

    private val enfermedadHealthy = EnfermedadEntity(
        id = 2,
        labelCnn = "healthy",
        nombre = "Planta sana",
        agenteCausal = "N/A",
        tipoAgente = "N/A",
        patronVisual = "Verde uniforme",
        impacto = "Sin impacto",
        prevencion = "Monitoreo regular",
        controlQuimico = "No requiere",
        controlBiologico = "No requiere",
        imagenReferencia = "healthy_normal",
        imagenGradcam = "healthy_gradcam",
        fuentes = "CIP 2023"
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        analisisDao = database.analisisDao()
        enfermedadDao = database.enfermedadDao()

        // Insertar enfermedades semilla
        kotlinx.coroutines.runBlocking {
            enfermedadDao.insertAll(listOf(enfermedadLateBlight, enfermedadHealthy))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── PT-I05a — La relación @Relation resuelve la enfermedad correcta ──

    @Test
    fun relacionAnalisisConEnfermedad_resuelveDatosCorrectos() = runTest {
        // Insertar un análisis para late blight
        val analisis = AnalisisEntity(
            enfermedadId = 1,
            imagenCapturada = "/img/hoja_test.jpg",
            precision = 0.92f,
            fechaHora = System.currentTimeMillis()
        )
        val id = analisisDao.insert(analisis)

        // Recuperar con relación
        val resultado = analisisDao.getById(id)

        assertNotNull("El resultado no debe ser null", resultado)
        assertEquals("La enfermedad asociada debe ser 'Tizón tardío'",
            "Tizón tardío", resultado!!.enfermedad.nombre)
        assertEquals("El label CNN debe ser 'late blight'",
            "late blight", resultado.enfermedad.labelCnn)
        assertEquals("El agente causal debe coincidir",
            "Phytophthora infestans", resultado.enfermedad.agenteCausal)
    }

    // ── PT-I05b — FK CASCADE: eliminar enfermedad elimina sus análisis ──

    @Test
    fun cascadeDelete_eliminarEnfermedadEliminaSusAnalisis() = runTest {
        // Insertar 2 análisis para late blight (id=1)
        analisisDao.insert(AnalisisEntity(
            enfermedadId = 1, imagenCapturada = "/img/1.jpg", precision = 0.90f
        ))
        analisisDao.insert(AnalisisEntity(
            enfermedadId = 1, imagenCapturada = "/img/2.jpg", precision = 0.85f
        ))
        // Insertar 1 análisis para healthy (id=2)
        analisisDao.insert(AnalisisEntity(
            enfermedadId = 2, imagenCapturada = "/img/3.jpg", precision = 0.95f
        ))

        // Verificar que hay 3 análisis
        val antes = analisisDao.getAllAnalisis().first()
        assertEquals("Debe haber 3 análisis antes", 3, antes.size)

        // Eliminar la enfermedad late blight con SQL directo
        // (Room no expone un delete en EnfermedadDao, usamos query directo)
        database.openHelper.writableDatabase.execSQL("DELETE FROM enfermedades WHERE id = 1")

        // Verificar que solo queda 1 análisis (el de healthy)
        val despues = analisisDao.getAllAnalisis().first()
        assertEquals(
            "CASCADE debe haber eliminado los 2 análisis de late blight, quedando 1",
            1, despues.size
        )
        assertEquals("El análisis restante debe ser de healthy",
            "Planta sana", despues[0].enfermedad.nombre)
    }

    // ── PT-I05c — Orden cronológico inverso ──

    @Test
    fun ordenCronologico_masRecientePrimero() = runTest {
        val baseTime = System.currentTimeMillis()

        // Insertar en orden cronológico (el más antiguo primero)
        analisisDao.insert(AnalisisEntity(
            enfermedadId = 1, imagenCapturada = "/img/antiguo.jpg",
            precision = 0.80f, fechaHora = baseTime - 3_600_000 // 1h atrás
        ))
        analisisDao.insert(AnalisisEntity(
            enfermedadId = 2, imagenCapturada = "/img/medio.jpg",
            precision = 0.85f, fechaHora = baseTime - 1_800_000 // 30min atrás
        ))
        analisisDao.insert(AnalisisEntity(
            enfermedadId = 1, imagenCapturada = "/img/reciente.jpg",
            precision = 0.92f, fechaHora = baseTime // ahora
        ))

        val lista = analisisDao.getAllAnalisis().first()

        assertEquals("Debe haber 3 registros", 3, lista.size)
        // El más reciente (fechaHora mayor) debe ser el primero (ORDER BY DESC)
        assertTrue(
            "El primer registro debe tener fechaHora >= el segundo",
            lista[0].analisis.fechaHora >= lista[1].analisis.fechaHora
        )
        assertTrue(
            "El segundo registro debe tener fechaHora >= el tercero",
            lista[1].analisis.fechaHora >= lista[2].analisis.fechaHora
        )
    }

    // ── PT-I05d — Múltiples análisis para una misma enfermedad ──

    @Test
    fun multiplesAnalisisParaUnaEnfermedad_todosSeRecuperan() = runTest {
        // Insertar 3 análisis todos con enfermedadId = 1
        repeat(3) { i ->
            analisisDao.insert(AnalisisEntity(
                enfermedadId = 1,
                imagenCapturada = "/img/multiple_$i.jpg",
                precision = 0.75f + (i * 0.05f)
            ))
        }

        val lista = analisisDao.getAllAnalisis().first()
        assertEquals("Deben recuperarse 3 análisis", 3, lista.size)

        // Todos deben estar asociados a la misma enfermedad
        lista.forEach { item ->
            assertEquals(
                "Todos deben estar asociados a 'Tizón tardío'",
                "Tizón tardío", item.enfermedad.nombre
            )
        }
    }

    // ── PT-I05e — getAllAnalisisList (suspend, sin Flow) ──

    @Test
    fun getAllAnalisisList_devuelveListaSinFlow() = runTest {
        analisisDao.insert(AnalisisEntity(
            enfermedadId = 1, imagenCapturada = "/img/suspend.jpg", precision = 0.88f
        ))
        analisisDao.insert(AnalisisEntity(
            enfermedadId = 2, imagenCapturada = "/img/suspend2.jpg", precision = 0.91f
        ))

        // Usar la versión suspend (no Flow) para obtener la lista
        val lista = analisisDao.getAllAnalisisList()

        assertEquals("Debe devolver 2 registros", 2, lista.size)
    }
}
