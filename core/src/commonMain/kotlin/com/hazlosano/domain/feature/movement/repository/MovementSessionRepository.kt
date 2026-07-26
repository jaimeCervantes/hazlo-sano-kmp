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

    /**
     * Refreshes the summary the history lists a session by.
     *
     * The distance is derivable from the stored points, but the history must not read every point of
     * every session to draw a list, so it is kept as a summary. Opening a session measures its route
     * again and heals that summary, which is what keeps the list and the detail from disagreeing
     * after the measurement improves.
     */
    suspend fun updateDistance(sessionId: Long, distanceMeters: Double)
}
