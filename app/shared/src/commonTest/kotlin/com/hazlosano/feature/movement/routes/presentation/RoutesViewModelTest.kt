package com.hazlosano.feature.movement.routes.presentation

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.parser.GpxFormat
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import com.hazlosano.domain.feature.movement.usecase.ExportRouteAsGpxUseCase
import com.hazlosano.domain.feature.movement.usecase.ImportRouteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoutesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `importing a file stores the route and says so`() = runTest(dispatcher) {
        val repository = FakeRoutes()
        val viewModel = viewModel(repository)

        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        assertEquals(1, repository.stored.size)
        assertEquals("Subida al cerro", repository.stored.single().name)
        assertEquals(RoutesMessage.Imported("Subida al cerro"), viewModel.uiState.value.message)
    }

    @Test
    fun `a file whose track has no name is called after the file`() = runTest(dispatcher) {
        // "Ruta sin nombre" in a list of routes helps nobody find the one they just imported.
        val repository = FakeRoutes()
        val viewModel = viewModel(repository)

        viewModel.import("Vuelta al lago.gpx", gpxNamed(null))
        testScheduler.advanceUntilIdle()

        assertEquals("Vuelta al lago", repository.stored.single().name)
    }

    @Test
    fun `an unreadable file reports the problem instead of storing anything`() =
        runTest(dispatcher) {
            val repository = FakeRoutes()
            val viewModel = viewModel(repository)

            viewModel.import("roto.gpx", "no soy un gpx".encodeToByteArray())
            testScheduler.advanceUntilIdle()

            assertTrue(repository.stored.isEmpty())
            assertNotNull(viewModel.uiState.value.message)
        }

    @Test
    fun `a track already stored asks before replacing it`() = runTest(dispatcher) {
        val repository = FakeRoutes()
        val viewModel = viewModel(repository)
        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        viewModel.import("cerro-copia.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        val pending = assertNotNull(viewModel.uiState.value.pendingImport)
        assertEquals("Subida al cerro", pending.existingName)
        assertEquals(1, repository.stored.size, "the duplicate was stored without asking")
    }

    @Test
    fun `confirming the replacement keeps one route rather than two`() = runTest(dispatcher) {
        val repository = FakeRoutes()
        val viewModel = viewModel(repository)
        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()
        viewModel.import("cerro-copia.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        viewModel.confirmReplace()
        testScheduler.advanceUntilIdle()

        assertEquals(1, repository.stored.size)
        assertNull(viewModel.uiState.value.pendingImport)
    }

    @Test
    fun `declining the replacement leaves what was already there`() = runTest(dispatcher) {
        val repository = FakeRoutes()
        val viewModel = viewModel(repository)
        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()
        viewModel.import("cerro-copia.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        viewModel.dismissPendingImport()

        assertNull(viewModel.uiState.value.pendingImport)
        assertEquals(1, repository.stored.size)
    }

    @Test
    fun `renaming to nothing is refused rather than stored`() = runTest(dispatcher) {
        val repository = FakeRoutes()
        val viewModel = viewModel(repository)
        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        viewModel.rename(routeId = 1, name = "   ")
        testScheduler.advanceUntilIdle()

        assertEquals("Subida al cerro", repository.stored.single().name)
        // El caso, no la frase: la redacción vive en el catálogo desde este slice.
        assertEquals(RoutesMessage.NameRequired, viewModel.uiState.value.message)
    }

    @Test
    fun `exporting hands the screen a file to write`() = runTest(dispatcher) {
        val repository = FakeRoutes()
        val viewModel = viewModel(repository)
        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        viewModel.export(routeId = 1)
        testScheduler.advanceUntilIdle()

        val exported = assertNotNull(viewModel.uiState.value.exported)
        assertEquals("Subida-al-cerro.gpx", exported.fileName)
        assertEquals("Subida al cerro", GpxFormat.parse(exported.gpx).name)
    }

    @Test
    fun `an export is handed over once`() = runTest(dispatcher) {
        // Otherwise returning to the screen opens the file picker again for a route already saved.
        val repository = FakeRoutes()
        val viewModel = viewModel(repository)
        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()
        viewModel.export(routeId = 1)
        testScheduler.advanceUntilIdle()

        viewModel.consumeExport()

        assertNull(viewModel.uiState.value.exported)
    }

    // ─────────── La espera de la importación ───────────
    //
    // Spec: `features/pulido_de_movimiento.feature`, slice 1. El hueco que estas pruebas cubren es
    // el que se vive: entre pedir el archivo y verlo importado, la pantalla no decía nada.

    /**
     * La espera empieza **al pedir** el archivo, no al recibirlo.
     *
     * Es la parte contraintuitiva y la que se midió: parsear y guardar 20.000 puntos son 378 ms,
     * mientras que el selector del sistema puede tardar segundos si baja el archivo de la nube.
     * Empezar a esperar cuando llegan los bytes dejaría sin explicar justo el trozo que se sufre.
     */
    @Test
    fun `asking for a file is already waiting`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeRoutes())

        viewModel.importRequested()

        assertTrue(viewModel.uiState.value.isImporting)
    }

    /**
     * La fila que la lista enseña mientras entra sabe de qué archivo es, en cuanto se sabe.
     *
     * Antes de que el selector entregue nada no hay nombre que dar, y entonces la fila lo dice en
     * vez de inventarse uno. El nombre de verdad sale de dentro del GPX y puede no parecerse al del
     * archivo, así que esto es lo que se sabe y no una promesa de lo que va a salir.
     */
    @Test
    fun `the row that is coming in takes the name of the file`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeRoutes())

        viewModel.importRequested()
        assertNull(viewModel.uiState.value.importing?.fileName, "todavía no hay archivo")

        viewModel.import("Vuelta al lago.gpx", gpxNamed("Vuelta al lago"))

        assertEquals("Vuelta al lago", viewModel.uiState.value.importing?.fileName)
    }

    @Test
    fun `a stored route ends the wait`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeRoutes())
        viewModel.importRequested()

        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImporting)
    }

    /** El desenlace que más se olvida de apagar la espera es el del error. */
    @Test
    fun `a file that cannot be read ends the wait too`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeRoutes())
        viewModel.importRequested()

        viewModel.import("roto.gpx", "no soy un gpx".encodeToByteArray())
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImporting)
        assertNotNull(viewModel.uiState.value.message)
    }

    /** Un duplicado deja de esperar a un archivo y pasa a esperar a una persona. */
    @Test
    fun `a duplicate ends the wait and asks instead`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeRoutes())
        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        viewModel.import("cerro-copia.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImporting)
        assertNotNull(viewModel.uiState.value.pendingImport)
    }

    @Test
    fun `replacing a duplicate is a wait of its own`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeRoutes())
        viewModel.import("cerro.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()
        viewModel.import("cerro-copia.gpx", gpxNamed("Subida al cerro"))
        testScheduler.advanceUntilIdle()

        viewModel.confirmReplace()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isImporting, "la espera se quedó encendida")
    }

    /**
     * Cerrar el selector sin elegir es lo normal —cambiar de idea— y tiene que apagar la espera.
     *
     * Sin esto, la pantalla se queda diciendo «importando» para siempre por un archivo que nadie
     * llegó a mandar, y el botón de importar apagado con ella.
     */
    @Test
    fun `closing the picker without choosing ends the wait`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeRoutes())
        viewModel.importRequested()

        viewModel.importAbandoned()

        assertFalse(viewModel.uiState.value.isImporting)
    }

    private fun viewModel(repository: RouteRepository): RoutesViewModel = RoutesViewModel(
        routes = repository,
        // El caso de uso lee el archivo fuera del hilo del llamante. En un test hay que darle el
        // dispatcher de prueba o el trabajo se escapa del reloj virtual de `runTest` y las
        // aserciones corren antes de que haya terminado.
        importRoute = ImportRouteUseCase(repository, GpxFormat, UnconfinedTestDispatcher()),
        exportRoute = ExportRouteAsGpxUseCase(repository),
    )

    private fun gpxNamed(name: String?): ByteArray {
        val nameTag = name?.let { "<name>$it</name>" }.orEmpty()
        return (
            """<gpx><trk>$nameTag<trkseg>""" +
                """<trkpt lat="19.43" lon="-99.13"><ele>2200</ele></trkpt>""" +
                """<trkpt lat="19.44" lon="-99.14"><ele>2210</ele></trkpt>""" +
                """</trkseg></trk></gpx>"""
            ).encodeToByteArray()
    }

    /** Stores routes the way the real repository does: by id, replacing rather than appending. */
    private class FakeRoutes : RouteRepository {
        private val state = MutableStateFlow<List<Route>>(emptyList())
        val stored: List<Route> get() = state.value

        override fun getAllRoutes(): Flow<List<Route>> = state
        override fun getRouteWithPoints(routeId: Long): Flow<Route?> =
            state.map { routes -> routes.firstOrNull { it.id == routeId } }

        override suspend fun getRouteByName(name: String): Route? =
            state.value.firstOrNull { it.name == name }

        override suspend fun getRouteByFingerprint(fingerprint: String): Route? =
            if (fingerprint.isBlank()) {
                null
            } else {
                state.value.firstOrNull { it.fingerprint == fingerprint }
            }

        override suspend fun saveRoute(route: Route): Long {
            val existing = state.value.indexOfFirst { it.id == route.id && route.id != 0L }
            return if (existing >= 0) {
                state.value = state.value.toMutableList().also { it[existing] = route }
                route.id
            } else {
                val id = (state.value.maxOfOrNull { it.id } ?: 0L) + 1
                state.value = state.value + route.copy(id = id)
                id
            }
        }

        override suspend fun renameRoute(routeId: Long, name: String) {
            state.value = state.value.map { if (it.id == routeId) it.copy(name = name) else it }
        }

        override suspend fun deleteRoute(routeId: Long) {
            state.value = state.value.filterNot { it.id == routeId }
        }
    }
}
