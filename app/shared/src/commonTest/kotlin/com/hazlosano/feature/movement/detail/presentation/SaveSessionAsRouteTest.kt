package com.hazlosano.feature.movement.detail.presentation

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.RouteProblem
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionDetailUseCase
import com.hazlosano.domain.feature.movement.usecase.RefreshSessionSummaryUseCase
import com.hazlosano.domain.feature.movement.usecase.SaveRouteFromSessionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Keeping an outing from the history as a route, from the screen that shows it. */
@OptIn(ExperimentalCoroutinesApi::class)
class SaveSessionAsRouteTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `an outing is kept under the name that was chosen rather than the session's own`() = runTest {
        val routes = FakeRoutes()
        val viewModel = buildViewModel(routes)

        viewModel.saveAsRoute("Subida al cerro")

        assertEquals("Subida al cerro", routes.saved.single().name)
        assertEquals(3, routes.saved.single().points.size)
        assertEquals(
            SaveRouteMessage.Saved("Subida al cerro"),
            viewModel.saveRouteMessage.value,
        )
    }

    @Test
    fun `saving without a name keeps nothing and says why`() = runTest {
        val routes = FakeRoutes()
        val viewModel = buildViewModel(routes)

        viewModel.saveAsRoute("  ")

        assertTrue(routes.saved.isEmpty())
        // El caso, no la frase: quien decide las palabras es la UI.
        assertEquals(
            SaveRouteMessage.Failed(RouteProblem.NAME_REQUIRED),
            viewModel.saveRouteMessage.value,
        )
    }

    @Test
    fun `the message is said once`() = runTest {
        val viewModel = buildViewModel(FakeRoutes())
        viewModel.saveAsRoute("Subida al cerro")

        viewModel.consumeSaveRouteMessage()

        assertEquals(null, viewModel.saveRouteMessage.value)
    }

    @Test
    fun `without somewhere to keep routes the option is not offered`() = runTest {
        // Reporting success and storing nothing is worse than not offering the button at all.
        val viewModel = SessionDetailViewModel(
            sessionId = SESSION,
            getSessionDetail = GetSessionDetailUseCase(sessions()),
            refreshSessionSummary = RefreshSessionSummaryUseCase(sessions()),
            saveRouteFromSession = null,
        )

        assertFalse(viewModel.canSaveAsRoute)
        viewModel.saveAsRoute("Da igual")
        assertEquals(null, viewModel.saveRouteMessage.value)
    }

    private fun buildViewModel(routes: RouteRepository): SessionDetailViewModel {
        val sessions = sessions()
        return SessionDetailViewModel(
            sessionId = SESSION,
            getSessionDetail = GetSessionDetailUseCase(sessions),
            refreshSessionSummary = RefreshSessionSummaryUseCase(sessions),
            saveRouteFromSession = SaveRouteFromSessionUseCase(routes, sessions),
        )
    }

    private fun sessions() = StubSessions(
        session = MovementSession(
            id = SESSION,
            routeId = null,
            name = "Salida del 9 ago",
            date = 1_786_255_200_000L,
            elapsedTime = 600,
            distanceTraveled = 1_250.0,
            previewPoints = emptyList(),
        ),
        path = listOf(
            UserLocation(19.4300, -99.13, altitude = 2_200.0, accuracy = 8f, timestamp = 0),
            UserLocation(19.4310, -99.13, altitude = 2_210.0, accuracy = 8f, timestamp = 30_000),
            UserLocation(19.4320, -99.13, altitude = 2_220.0, accuracy = 8f, timestamp = 60_000),
        ),
    )

    private class StubSessions(
        private val session: MovementSession,
        private val path: List<UserLocation>,
    ) : MovementSessionRepository {
        override fun getAllSessions(): Flow<List<MovementSession>> = flowOf(listOf(session))
        override fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> = flowOf(path)
        override suspend fun saveSession(
            session: MovementSession,
            rawPoints: List<UserLocation>,
        ) = Unit
        override suspend fun updateDistance(sessionId: Long, distanceMeters: Double) = Unit
    }

    private class FakeRoutes : RouteRepository {
        val saved = mutableListOf<Route>()
        override fun getAllRoutes(): Flow<List<Route>> = flowOf(saved)
        override fun getRouteWithPoints(routeId: Long): Flow<Route?> = flowOf(null)
        override suspend fun getRouteByName(name: String): Route? = null
        override suspend fun getRouteByFingerprint(fingerprint: String): Route? = null
        override suspend fun saveRoute(route: Route): Long {
            saved += route
            return saved.size.toLong()
        }
        override suspend fun renameRoute(routeId: Long, name: String) = Unit
        override suspend fun deleteRoute(routeId: Long) = Unit
    }

    private companion object {
        const val SESSION = 7L
    }
}
