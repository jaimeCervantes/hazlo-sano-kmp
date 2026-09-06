package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.RouteStanding
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * El aviso de desvío, ya en la pantalla: la parte que junta «te has salido» con «te estás moviendo».
 *
 * La regla del movimiento es lo que hace que esto no mienta, y salió de una traza de campo: con el
 * teléfono quieto bajo techo la señal llega a colocarse a 291 m del sitio donde está.
 *
 * Spec: `features/movement_route_deviation.feature`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RouteDeviationOnTheTrackerTest {

    /** Un tramo recto de norte a sur; a esta latitud un grado de longitud son ~105 km. */
    private val route = Route(
        id = 4,
        name = "Cañada del Molino",
        distance = 1_100.0,
        elevationGain = null,
        points = listOf(
            WayPoint(latitude = 19.4300, longitude = -99.1300),
            WayPoint(latitude = 19.4400, longitude = -99.1300),
        ),
    )

    private fun metersEastOfRoute(meters: Double): Double = -99.1300 + meters / 105_000.0

    private fun at(longitude: Double, atMillis: Long) =
        UserLocation(latitude = 19.4350, longitude = longitude, timestamp = atMillis)

    private val locations = MutableSharedFlow<UserLocation>(replay = 1)

    private val locationRepository = object : LocationRepository {
        override fun getLocationUpdates(): Flow<UserLocation> = locations
    }

    private lateinit var controller: FakeRecordingController

    private fun tracker(): TrackerViewModel {
        controller = FakeRecordingController()
        return TrackerViewModel(
            locationRepository = locationRepository,
            recordingController = controller,
            routes = FakeRoutes(listOf(route)),
        )
    }

    /**
     * Pone la grabación en marcha con el recorrido creciendo, que es lo que `isMoving` mira: la
     * distancia entre la última lectura y el último punto del recorrido.
     */
    private fun FakeRecordingController.moving(atMillis: Long) {
        publish(
            RecordingState(
                isRecording = true,
                startedAtMillis = 0,
                traveledPoints = listOf(at(-99.1300, atMillis)),
                lastReadingAtMillis = atMillis,
            ),
        )
    }

    /** El recorrido dejó de crecer hace mucho, aunque el receptor sigue hablando. */
    private fun FakeRecordingController.standingStill(atMillis: Long) {
        publish(
            RecordingState(
                isRecording = true,
                startedAtMillis = 0,
                traveledPoints = listOf(at(-99.1300, 0)),
                lastReadingAtMillis = atMillis,
            ),
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `with no route there is nothing to say`() = runTest {
        val model = tracker()
        model.startTracking()
        controller.moving(atMillis = 1_000)

        locations.emit(at(metersEastOfRoute(5_000.0), atMillis = 1_000))

        assertEquals(RouteStanding.Unknown, model.routeStanding.value)
    }

    @Test
    fun `following a route and staying on it says so`() = runTest {
        val model = tracker()
        model.startTracking()
        model.followRoute(routeId = 4)
        model.followedRoute.first { it != null }
        controller.moving(atMillis = 1_000)

        locations.emit(at(-99.1300, atMillis = 1_000))

        assertIs<RouteStanding.OnRoute>(model.routeStanding.value)
    }

    /** El caso que este slice existe para cubrir. */
    @Test
    fun `straying far enough for long enough warns`() = runTest {
        val model = tracker()
        model.startTracking()
        model.followRoute(routeId = 4)
        model.followedRoute.first { it != null }
        val away = metersEastOfRoute(300.0)

        controller.moving(atMillis = 0)
        locations.emit(at(away, atMillis = 0))
        controller.moving(atMillis = 30_000)
        locations.emit(at(away, atMillis = 30_000))

        val standing = assertIs<RouteStanding.OffRoute>(model.routeStanding.value)
        assertEquals(300.0, standing.metersFromRoute, 10.0)
    }

    /**
     * La regla que produjo la cuarta traza: parado, la distancia al trazado es ruido. Sin esto, media
     * hora con el teléfono en una mesa avisaría de un desvío de 291 m que nunca ocurrió.
     */
    @Test
    fun `standing still never warns however far the signal wanders`() = runTest {
        val model = tracker()
        model.startTracking()
        model.followRoute(routeId = 4)
        model.followedRoute.first { it != null }
        val away = metersEastOfRoute(291.5)

        controller.standingStill(atMillis = 120_000)
        locations.emit(at(away, atMillis = 120_000))
        controller.standingStill(atMillis = 180_000)
        locations.emit(at(away, atMillis = 180_000))

        assertEquals(RouteStanding.Unknown, model.routeStanding.value)
    }

    @Test
    fun `dropping the route stops judging`() = runTest {
        val model = tracker()
        model.startTracking()
        model.followRoute(routeId = 4)
        model.followedRoute.first { it != null }
        val away = metersEastOfRoute(300.0)
        controller.moving(atMillis = 0)
        locations.emit(at(away, atMillis = 0))
        controller.moving(atMillis = 30_000)
        locations.emit(at(away, atMillis = 30_000))
        assertIs<RouteStanding.OffRoute>(model.routeStanding.value)

        model.stopFollowingRoute()

        assertEquals(RouteStanding.Unknown, model.routeStanding.value)
    }
}
