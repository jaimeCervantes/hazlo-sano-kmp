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

    private fun session(id: Long): MovementSession =
        MovementSession(
            id = id,
            routeId = null,
            name = "Sesión $id",
            date = 1_784_877_300_000L,
            elapsedTime = 600,
            distanceTraveled = 1_250.0,
            elevationGain = 12.0,
            previewPoints = emptyList(),
            stats = SessionStats(),
        )
}
