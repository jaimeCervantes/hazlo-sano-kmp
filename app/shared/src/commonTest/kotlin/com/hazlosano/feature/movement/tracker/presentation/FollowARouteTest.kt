package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import com.hazlosano.domain.feature.movement.repository.RecordingController
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private object NoLocations : LocationRepository {
    override fun getLocationUpdates(): Flow<UserLocation> = emptyFlow()
}

/**
 * Salir siguiendo una ruta.
 *
 * `MovementSessionEntity.routeId` llevaba desde que existe guardando siempre `null`, porque nadie
 * tenía cómo decírselo. Estos tests cuidan el viaje entero: elegir la ruta, cargarla y que la
 * grabación arranque sabiendo con cuál sale.
 *
 * **No hay seguimiento.** Con la ruta cargada se dibuja y se recuerda; avisar de un desvío es C3.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FollowARouteTest {

    private val cañadaDelMolino = Route(
        id = 4,
        name = "Cañada del Molino",
        distance = 5_400.0,
        elevationGain = 210.0,
        points = listOf(
            WayPoint(latitude = 19.4300, longitude = -99.1300, altitude = 2_200.0),
            WayPoint(latitude = 19.4320, longitude = -99.1310, altitude = 2_260.0),
        ),
    )

    private fun viewModel(
        controller: RecordingController,
        routes: RouteRepository = FakeRoutes(),
    ) = TrackerViewModel(
        locationRepository = NoLocations,
        recordingController = controller,
        routes = routes,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `an outing starts with no route unless one is chosen`() = runTest {
        val controller = FakeRecordingController()

        viewModel(controller).startRecording()

        assertEquals(listOf<Long?>(null), controller.startedWithRouteId)
    }

    @Test
    fun `choosing a route loads it with its points`() = runTest {
        val model = viewModel(FakeRecordingController(), FakeRoutes(listOf(cañadaDelMolino)))

        model.followRoute(routeId = 4)

        val followed = model.followedRoute.first { it != null }!!
        assertEquals("Cañada del Molino", followed.name)
        assertEquals(
            listOf(19.4300 to -99.1300, 19.4320 to -99.1310),
            followed.points.map { it.latitude to it.longitude },
        )
    }

    /** Lo que cierra el círculo: la sesión recuerda con qué ruta salió. */
    @Test
    fun `the outing remembers the route it went out with`() = runTest {
        val controller = FakeRecordingController()
        val model = viewModel(controller, FakeRoutes(listOf(cañadaDelMolino)))
        model.followRoute(routeId = 4)
        model.followedRoute.first { it != null }

        model.startRecording()

        assertEquals(listOf<Long?>(4), controller.startedWithRouteId)
    }

    @Test
    fun `dropping the route goes back to an outing with none`() = runTest {
        val controller = FakeRecordingController()
        val model = viewModel(controller, FakeRoutes(listOf(cañadaDelMolino)))
        model.followRoute(routeId = 4)
        model.followedRoute.first { it != null }

        model.stopFollowingRoute()
        model.startRecording()

        assertNull(model.followedRoute.value)
        assertEquals(listOf<Long?>(null), controller.startedWithRouteId)
    }

    /** Una ruta que ya no está no puede dejar la pantalla a medias ni tumbarla. */
    @Test
    fun `choosing a route that is gone leaves the outing without one`() = runTest {
        val model = viewModel(FakeRecordingController(), FakeRoutes(listOf(cañadaDelMolino)))

        model.followRoute(routeId = 99)

        assertNull(model.followedRoute.value)
    }

    /** La traza de diagnóstico y la ruta son decisiones distintas y viajan las dos. */
    @Test
    fun `the diagnostic trace and the route travel together`() = runTest {
        val controller = FakeRecordingController()
        val model = viewModel(controller, FakeRoutes(listOf(cañadaDelMolino)))
        model.followRoute(routeId = 4)
        model.followedRoute.first { it != null }

        model.setCaptureTrace(true)
        model.startRecording()

        assertEquals(listOf<Long?>(4), controller.startedWithRouteId)
        assertEquals(true, controller.startedWithTraceCapture)
    }
}
