package com.hazlosano.feature.movement.routes.presentation

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.parser.GpxFormat
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import com.hazlosano.domain.feature.movement.usecase.ExportRouteAsGpxUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Lo que se puede hacer con una ruta sin salir de mirarla.
 *
 * Descargar y borrar vivían sólo en las filas de la lista, así que quien abría una ruta en el mapa
 * —que es donde se decide si sirve— tenía que volver atrás para hacer nada con ella.
 *
 * Spec: `features/pulido_de_movimiento.feature`, slice 1.
 */
class RouteDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a route can be written out from where it is being looked at`() = runTest(dispatcher) {
        val repository = FakeRoutes(listOf(route(id = 1, name = "Subida al cerro")))
        val viewModel = viewModel(repository, routeId = 1)

        viewModel.export()
        testScheduler.advanceUntilIdle()

        val exported = assertNotNull(viewModel.exported.value)
        assertEquals("Subida-al-cerro.gpx", exported.fileName)
        assertEquals("Subida al cerro", GpxFormat.parse(exported.gpx).name)
    }

    /** Igual que en la lista: si no se limpia, volver a la pantalla vuelve a ofrecer el guardado. */
    @Test
    fun `an export is handed over once`() = runTest(dispatcher) {
        val repository = FakeRoutes(listOf(route(id = 1, name = "Subida al cerro")))
        val viewModel = viewModel(repository, routeId = 1)
        viewModel.export()
        testScheduler.advanceUntilIdle()

        viewModel.consumeExport()

        assertNull(viewModel.exported.value)
    }

    @Test
    fun `deleting the route removes it`() = runTest(dispatcher) {
        val repository = FakeRoutes(listOf(route(id = 1, name = "Subida al cerro")))
        val viewModel = viewModel(repository, routeId = 1)

        viewModel.delete()
        testScheduler.advanceUntilIdle()

        assertTrue(repository.stored.isEmpty())
    }

    /**
     * Y lo dice, para que la pantalla pueda volver a la lista.
     *
     * Se anuncia en vez de deducirse de que el detalle se quede vacío: «no la encuentro» y «acabas
     * de borrarla» son dos situaciones distintas y merecen dos respuestas distintas.
     */
    @Test
    fun `deleting the route says so`() = runTest(dispatcher) {
        val repository = FakeRoutes(listOf(route(id = 1, name = "Subida al cerro")))
        val viewModel = viewModel(repository, routeId = 1)
        assertFalse(viewModel.deleted.value)

        viewModel.delete()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.deleted.value)
    }

    /** Mirar una ruta no la borra: sólo lo hace pedirlo. */
    @Test
    fun `looking at a route leaves it alone`() = runTest(dispatcher) {
        val repository = FakeRoutes(listOf(route(id = 1, name = "Subida al cerro")))
        val viewModel = viewModel(repository, routeId = 1)

        testScheduler.advanceUntilIdle()

        assertEquals(1, repository.stored.size)
        assertFalse(viewModel.deleted.value)
    }

    private fun viewModel(repository: RouteRepository, routeId: Long) = RouteDetailViewModel(
        routeId = routeId,
        routes = repository,
        exportRoute = ExportRouteAsGpxUseCase(repository),
    )

    private fun route(id: Long, name: String) = Route(
        id = id,
        name = name,
        distance = 1_200.0,
        elevationGain = 45.0,
        points = listOf(
            WayPoint(latitude = 19.4300, longitude = -99.1300, altitude = 2_200.0),
            WayPoint(latitude = 19.4310, longitude = -99.1310, altitude = 2_245.0),
        ),
    )

    private class FakeRoutes(initial: List<Route> = emptyList()) : RouteRepository {
        private val state = MutableStateFlow(initial)
        val stored: List<Route> get() = state.value

        override fun getAllRoutes(): Flow<List<Route>> = state

        override fun getRouteWithPoints(routeId: Long): Flow<Route?> =
            state.map { routes -> routes.firstOrNull { it.id == routeId } }

        override suspend fun getRouteByName(name: String): Route? =
            state.value.firstOrNull { it.name == name }

        override suspend fun getRouteByFingerprint(fingerprint: String): Route? = null

        override suspend fun saveRoute(route: Route): Long = route.id

        override suspend fun renameRoute(routeId: Long, name: String) {
            state.value = state.value.map { if (it.id == routeId) it.copy(name = name) else it }
        }

        override suspend fun deleteRoute(routeId: Long) {
            state.value = state.value.filterNot { it.id == routeId }
        }
    }
}
