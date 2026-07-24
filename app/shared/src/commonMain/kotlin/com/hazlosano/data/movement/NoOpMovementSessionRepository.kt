package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Used on platforms without an initialized database (recording is Android-only for now). */
object NoOpMovementSessionRepository : MovementSessionRepository {
    override fun getAllSessions(): Flow<List<MovementSession>> = flowOf(emptyList())
    override fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> = flowOf(emptyList())
    override suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>) = Unit
}
