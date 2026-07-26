package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.SessionDetail
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class RecordingRepository : MovementSessionRepository {
    val distanceUpdates: MutableList<Pair<Long, Double>> = mutableListOf()

    override fun getAllSessions(): Flow<List<MovementSession>> = flowOf(emptyList())
    override fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> = flowOf(emptyList())
    override suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>) = Unit

    override suspend fun updateDistance(sessionId: Long, distanceMeters: Double) {
        distanceUpdates += sessionId to distanceMeters
    }
}

class RefreshSessionSummaryUseCaseTest {

    @Test
    fun `a better measurement replaces the summary the history lists`() = runTest {
        val repository = RecordingRepository()

        RefreshSessionSummaryUseCase(repository)(
            detail(storedDistance = 1_250.0, measuredDistance = 1_180.0, points = 2),
        )

        assertEquals(listOf(SESSION_ID to 1_180.0), repository.distanceUpdates)
    }

    @Test
    fun `a summary that already agrees is left alone`() = runTest {
        val repository = RecordingRepository()

        RefreshSessionSummaryUseCase(repository)(
            detail(storedDistance = 1_250.0, measuredDistance = 1_250.0, points = 2),
        )

        assertTrue(repository.distanceUpdates.isEmpty(), "the history was rewritten for nothing")
    }

    @Test
    fun `floating point noise is not a better measurement`() = runTest {
        val repository = RecordingRepository()

        RefreshSessionSummaryUseCase(repository)(
            detail(storedDistance = 1_250.0, measuredDistance = 1_250.2, points = 2),
        )

        assertTrue(repository.distanceUpdates.isEmpty())
    }

    @Test
    fun `a session that stored no route keeps the only distance it has`() = runTest {
        // Measuring an empty route gives zero, which is the absence of a route rather than a
        // journey of no length. Writing that back would destroy the only record of how far it went.
        val repository = RecordingRepository()

        RefreshSessionSummaryUseCase(repository)(
            detail(storedDistance = 1_250.0, measuredDistance = 0.0, points = 0),
        )

        assertTrue(repository.distanceUpdates.isEmpty(), "an empty route erased a real distance")
    }

    private fun detail(
        storedDistance: Double,
        measuredDistance: Double,
        points: Int,
    ): SessionDetail = SessionDetail(
        session = MovementSession(
            id = SESSION_ID,
            routeId = null,
            name = "Sesión",
            date = 0L,
            elapsedTime = 600,
            distanceTraveled = storedDistance,
            previewPoints = emptyList(),
        ),
        path = List(points) { UserLocation(latitude = 19.43, longitude = -99.13) },
        distanceMeters = measuredDistance,
    )
}

private const val SESSION_ID = 7L
