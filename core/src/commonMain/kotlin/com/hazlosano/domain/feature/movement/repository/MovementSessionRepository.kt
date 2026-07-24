package com.hazlosano.domain.feature.movement.repository

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for recorded movement sessions. Segregated from the broader [RouteRepository]
 * (routes/GPX) so consumers that only record sessions depend on this narrow contract (ISP).
 */
interface MovementSessionRepository {
    fun getAllSessions(): Flow<List<MovementSession>>
    fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>>
    suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>)
}
