package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.parser.GpxFormat
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Keeping an outing you recorded as a route you can follow again, under a name you choose. */
class SaveRouteFromSessionUseCaseTest {

    @Test
    fun `an outing becomes a named route with its own measurements`() = runTest {
        val routes = FakeRouteRepository()
        val useCase = SaveRouteFromSessionUseCase(routes, FakeSessions(walkUphill()))

        val result = useCase(sessionId = 1, name = "Subida al cerro")

        assertTrue(result is SaveRouteFromSessionUseCase.Result.Success)
        assertEquals("Subida al cerro", result.route.name)
        assertEquals(3, result.route.points.size)
        assertTrue(result.route.distance > 0.0, "the route measured no distance")
        assertEquals(20.0, result.route.elevationGain, 0.001)
        assertTrue(result.route.fingerprint?.isNotBlank() == true)
        assertEquals(1, routes.saved.size)
    }

    @Test
    fun `the stored id comes back on the route`() = runTest {
        val routes = FakeRouteRepository(nextId = 42)
        val useCase = SaveRouteFromSessionUseCase(routes, FakeSessions(walkUphill()))

        val result = useCase(sessionId = 1, name = "Con id")

        assertEquals(42L, (result as SaveRouteFromSessionUseCase.Result.Success).route.id)
    }

    @Test
    fun `a route needs a name`() = runTest {
        val routes = FakeRouteRepository()
        val useCase = SaveRouteFromSessionUseCase(routes, FakeSessions(walkUphill()))

        val result = useCase(sessionId = 1, name = "   ")

        assertTrue(result is SaveRouteFromSessionUseCase.Result.Error)
        assertTrue(routes.saved.isEmpty(), "an unnamed route was stored anyway")
    }

    @Test
    fun `a single reading is a place rather than a route`() = runTest {
        val routes = FakeRouteRepository()
        val onePoint = listOf(reading(19.43, -99.13, altitude = 2200.0, at = 0))
        val useCase = SaveRouteFromSessionUseCase(routes, FakeSessions(onePoint))

        val result = useCase(sessionId = 1, name = "Un punto")

        assertTrue(result is SaveRouteFromSessionUseCase.Result.Error)
        assertTrue(routes.saved.isEmpty())
    }

    @Test
    fun `readings that carried no altitude make a route that carries none`() = runTest {
        // The altitude slice made a missing altitude say so. It has to survive becoming a route,
        // and survive again as GPX: an exported <ele> of zero would put the track at sea level.
        val routes = FakeRouteRepository()
        val flat = listOf(
            reading(19.43, -99.13, altitude = null, at = 0),
            reading(19.44, -99.13, altitude = null, at = 2_000),
        )
        val useCase = SaveRouteFromSessionUseCase(routes, FakeSessions(flat))

        val result = useCase(sessionId = 1, name = "Sin altitud")

        val route = (result as SaveRouteFromSessionUseCase.Result.Success).route
        assertTrue(route.points.all { it.altitude == null })
        assertEquals(0.0, route.elevationGain, 0.001)
        assertNull(GpxFormat.parse(GpxFormat.write(route)).points.first().altitude)
    }

    private fun walkUphill(): List<UserLocation> = listOf(
        reading(19.4300, -99.1300, altitude = 2_200.0, at = 0),
        reading(19.4310, -99.1300, altitude = 2_210.0, at = 2_000),
        reading(19.4320, -99.1300, altitude = 2_220.0, at = 4_000),
    )

    private fun reading(lat: Double, lon: Double, altitude: Double?, at: Long): UserLocation =
        UserLocation(
            latitude = lat,
            longitude = lon,
            altitude = altitude,
            accuracy = 8f,
            timestamp = at,
        )

    private class FakeSessions(private val points: List<UserLocation>) : MovementSessionRepository {
        override fun getAllSessions(): Flow<List<MovementSession>> = flowOf(emptyList())
        override fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> = flowOf(points)
        override suspend fun saveSession(
            session: MovementSession,
            rawPoints: List<UserLocation>,
        ) = Unit
        override suspend fun updateDistance(sessionId: Long, distanceMeters: Double) = Unit
    }

    private class FakeRouteRepository(private val nextId: Long = 1) : RouteRepository {
        val saved = mutableListOf<Route>()
        override fun getAllRoutes(): Flow<List<Route>> = flowOf(saved)
        override fun getRouteWithPoints(routeId: Long): Flow<Route?> =
            flowOf(saved.firstOrNull { it.id == routeId })
        override suspend fun getRouteByName(name: String): Route? = null
        override suspend fun getRouteByFingerprint(fingerprint: String): Route? = null
        override suspend fun saveRoute(route: Route): Long {
            saved += route
            return nextId
        }
        override suspend fun renameRoute(routeId: Long, name: String) = Unit
        override suspend fun deleteRoute(routeId: Long) = Unit
    }
}
