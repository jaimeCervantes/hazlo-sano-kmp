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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertTrue(viewModel.uiState.value.message.orEmpty().contains("Subida al cerro"))
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
        assertEquals("La ruta necesita un nombre.", viewModel.uiState.value.message)
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

    private fun viewModel(repository: RouteRepository): RoutesViewModel = RoutesViewModel(
        routes = repository,
        importRoute = ImportRouteUseCase(repository, GpxFormat),
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
