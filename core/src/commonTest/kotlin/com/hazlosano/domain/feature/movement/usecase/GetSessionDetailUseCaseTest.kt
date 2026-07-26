package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeMovementSessionRepository(
    private val sessions: List<MovementSession>,
    private val pointsBySession: Map<Long, List<UserLocation>> = emptyMap(),
) : MovementSessionRepository {
    override fun getAllSessions(): Flow<List<MovementSession>> = flowOf(sessions)

    override fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> =
        flowOf(pointsBySession[sessionId].orEmpty())

    override suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>) = Unit

    var distanceUpdates: MutableList<Pair<Long, Double>> = mutableListOf()

    override suspend fun updateDistance(sessionId: Long, distanceMeters: Double) {
        distanceUpdates += sessionId to distanceMeters
    }
}

class GetSessionDetailUseCaseTest {

    @Test
    fun `when the session exists then it is returned with its stored path`() = runTest {
        val path = listOf(
            UserLocation(latitude = 19.43, longitude = -99.13),
            UserLocation(latitude = 19.44, longitude = -99.13),
        )
        val useCase = GetSessionDetailUseCase(
            FakeMovementSessionRepository(
                sessions = listOf(session(id = 1), session(id = 2)),
                pointsBySession = mapOf(2L to path),
            ),
        )

        val detail = useCase(sessionId = 2).first()

        assertNotNull(detail)
        assertEquals(2, detail.session.id)
        assertEquals(path, detail.path)
        assertTrue(detail.hasPath)
    }

    @Test
    fun `when the session stored no path then it has no path to draw`() = runTest {
        val useCase = GetSessionDetailUseCase(
            FakeMovementSessionRepository(sessions = listOf(session(id = 1))),
        )

        val detail = useCase(sessionId = 1).first()

        assertNotNull(detail)
        assertFalse(detail.hasPath)
    }

    @Test
    fun `when a single point was stored then there is still no path to draw`() = runTest {
        val useCase = GetSessionDetailUseCase(
            FakeMovementSessionRepository(
                sessions = listOf(session(id = 1)),
                pointsBySession = mapOf(1L to listOf(UserLocation(latitude = 19.43, longitude = -99.13))),
            ),
        )

        val detail = useCase(sessionId = 1).first()

        assertNotNull(detail)
        assertFalse(detail.hasPath)
    }

    @Test
    fun `when the session does not exist then there is no detail`() = runTest {
        val useCase = GetSessionDetailUseCase(
            FakeMovementSessionRepository(sessions = listOf(session(id = 1))),
        )

        assertNull(useCase(sessionId = 404).first())
    }

    @Test
    fun `the figures are measured from the stored route rather than from what was saved`() = runTest {
        // A session recorded when the app measured worse: the stored summary says 1250 m, the route
        // it kept says something else. What the detail reports is the route.
        val path = listOf(
            UserLocation(latitude = 19.4300, longitude = -99.13, altitude = 2_000.0, timestamp = 0),
            UserLocation(latitude = 19.4310, longitude = -99.13, altitude = 2_030.0, timestamp = 20_000),
            UserLocation(latitude = 19.4320, longitude = -99.13, altitude = 2_060.0, timestamp = 40_000),
        )
        val useCase = GetSessionDetailUseCase(
            FakeMovementSessionRepository(
                sessions = listOf(session(id = 1)),
                pointsBySession = mapOf(1L to path),
            ),
        )

        val detail = assertNotNull(useCase(sessionId = 1).first())

        assertTrue(detail.distanceMeters > 0.0, "the route was not measured")
        assertNotEquals(
            detail.session.distanceTraveled,
            detail.distanceMeters,
            "this session's stored summary happens to match, so the test proves nothing",
        )
        assertNotNull(detail.stats.movingTime)
        assertNotNull(detail.stats.totalAscent)
    }

    @Test
    fun `a session that stored no route reports nothing measured`() = runTest {
        val useCase = GetSessionDetailUseCase(
            FakeMovementSessionRepository(sessions = listOf(session(id = 1))),
        )

        val detail = assertNotNull(useCase(sessionId = 1).first())

        assertEquals(SessionStats(), detail.stats)
        assertEquals(0.0, detail.distanceMeters)
    }

    private fun session(id: Long): MovementSession =
        MovementSession(
            id = id,
            routeId = null,
            name = "Sesión $id",
            date = 1_784_877_300_000L,
            elapsedTime = 600,
            distanceTraveled = 1_250.0,
            previewPoints = emptyList(),
        )
}
