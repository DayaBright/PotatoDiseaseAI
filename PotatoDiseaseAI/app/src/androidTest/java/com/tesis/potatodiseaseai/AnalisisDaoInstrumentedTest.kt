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
 * Pruebas instrumentadas para [AnalisisDao] con Room.
 *
 * Se usa una base de datos en memoria (inMemoryDatabaseBuilder) para que
 * cada prueba sea independiente y no afecte datos reales del dispositivo.
 *
 * Casos cubiertos:
 *  - PT-I01: Inserción y recuperación de un diagnóstico
 *  - PT-I02: Eliminación individual de un diagnóstico
 *  - PT-I03: Vaciado total con deleteAll()
 */
@RunWith(AndroidJUnit4::class)
class AnalisisDaoInstrumentedTest {

    private lateinit var database: AppDatabase
    private lateinit var analisisDao: AnalisisDao
    private lateinit var enfermedadDao: EnfermedadDao

    // ── Enfermedad semilla reutilizada por todos los tests ──────────────
    private val enfermedadSemilla = EnfermedadEntity(
        id = 1,
        labelCnn = "late_blight",
        nombre = "Tizón tardío",
        agenteCausal = "Phytophthora infestans",
        impacto = "Alto",
        manifestacionesVisuales = "Manchas oscuras irregulares",
        signosClave = "Halo amarillo en hojas",
        recomendaciones = "Fungicida sistémico|Rotación de cultivos",
        imagenReferencia = "img_late_blight_normal",
        imagenGradcam = "img_late_blight_gradcam",
        fuentes = "CIP 2023"
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Base de datos en memoria: se destruye al cerrar
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()   // Permitido solo en tests
            .build()

        analisisDao = database.analisisDao()
        enfermedadDao = database.enfermedadDao()

        // Insertar la enfermedad semilla para satisfacer la FK
        kotlinx.coroutines.runBlocking {
            enfermedadDao.insertAll(listOf(enfermedadSemilla))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ────────────────────────────────────────────────────────────────────
    // PT-I01 — Inserción y recuperación de diagnóstico
    // ────────────────────────────────────────────────────────────────────
    /**
     * Inserta un objeto [AnalisisEntity] con enfermedad_id=1, precision=0.95f
     * y una ruta de imagen, luego lo recupera por id y verifica que los
     * campos coinciden.
     */
    @Test
    fun insertarYRecuperarDiagnostico_datosCoinciden() = runTest {
        // Arrange
        val analisis = AnalisisEntity(
            enfermedadId = 1,
            imagenCapturada = "/storage/emulated/0/Pictures/potato_leaf_01.jpg",
            precision = 0.95f,
            fechaHora = System.currentTimeMillis()
        )

        // Act — insertar y obtener el ID generado
        val idInsertado = analisisDao.insert(analisis)
        val recuperado = analisisDao.getById(idInsertado)

        // Assert
        assertNotNull("El registro recuperado no debe ser null", recuperado)
        recuperado!!  // smart-cast tras assertNotNull
        assertEquals("El ID debe coincidir", idInsertado, recuperado.analisis.id)
        assertEquals("enfermedadId debe ser 1", 1L, recuperado.analisis.enfermedadId)
        assertEquals(
            "La ruta de imagen debe coincidir",
            "/storage/emulated/0/Pictures/potato_leaf_01.jpg",
            recuperado.analisis.imagenCapturada
        )
        assertEquals(
            "La precisión debe ser 0.95f",
            0.95f,
            recuperado.analisis.precision,
            0.001f          // delta de tolerancia para Float
        )
        // Verificar que la relación con enfermedad se resolvió
        assertEquals(
            "La enfermedad asociada debe ser 'Tizón tardío'",
            "Tizón tardío",
            recuperado.enfermedad.nombre
        )
    }

    // ────────────────────────────────────────────────────────────────────
    // PT-I02 — Eliminación individual
    // ────────────────────────────────────────────────────────────────────
    /**
     * Inserta dos registros en la tabla analisis, elimina el primero por id
     * y verifica que la consulta de todos los registros devuelve exactamente
     * uno.
     */
    @Test
    fun eliminarPorId_soloQuedaUnRegistro() = runTest {
        // Arrange — insertar dos análisis
        val analisis1 = AnalisisEntity(
            enfermedadId = 1,
            imagenCapturada = "/images/hoja_a.jpg",
            precision = 0.88f
        )
        val analisis2 = AnalisisEntity(
            enfermedadId = 1,
            imagenCapturada = "/images/hoja_b.jpg",
            precision = 0.72f
        )

        val id1 = analisisDao.insert(analisis1)
        val id2 = analisisDao.insert(analisis2)

        // Act — eliminar el primero
        analisisDao.deleteById(id1)

        // Assert — debe quedar exactamente 1 registro
        val listaRestante = analisisDao.getAllAnalisis().first()
        assertEquals(
            "Después de eliminar uno, debe quedar exactamente 1 registro",
            1,
            listaRestante.size
        )
        assertEquals(
            "El registro restante debe ser el segundo insertado",
            id2,
            listaRestante[0].analisis.id
        )
    }

    // ────────────────────────────────────────────────────────────────────
    // PT-I03 — Persistencia entre sesiones (vaciado total)
    // ────────────────────────────────────────────────────────────────────
    /**
     * Inserta 3 registros, llama a deleteAll() y verifica que la lista
     * resultante tiene tamaño 0.
     */
    @Test
    fun deleteAll_listaQuedaVacia() = runTest {
        // Arrange — insertar 3 análisis
        val registros = listOf(
            AnalisisEntity(enfermedadId = 1, imagenCapturada = "/img/1.jpg", precision = 0.91f),
            AnalisisEntity(enfermedadId = 1, imagenCapturada = "/img/2.jpg", precision = 0.85f),
            AnalisisEntity(enfermedadId = 1, imagenCapturada = "/img/3.jpg", precision = 0.78f)
        )
        registros.forEach { analisisDao.insert(it) }

        // Verificar que se insertaron 3
        val antesDeEliminar = analisisDao.getAllAnalisis().first()
        assertEquals(
            "Se deben haber insertado 3 registros",
            3,
            antesDeEliminar.size
        )

        // Act — eliminar todos
        analisisDao.deleteAll()

        // Assert — la lista debe estar vacía
        val despuesDeEliminar = analisisDao.getAllAnalisis().first()
        assertEquals(
            "Tras deleteAll() la lista debe tener tamaño 0",
            0,
            despuesDeEliminar.size
        )
    }
}
