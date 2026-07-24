package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Reads recorded sessions. Depends on the segregated [MovementSessionRepository] instead of the
 * broader routes/GPX contract, so the history only needs session persistence to exist.
 */
class GetSessionsUseCase(
    private val repository: MovementSessionRepository,
) {
    operator fun invoke(): Flow<List<MovementSession>> = repository.getAllSessions()

    fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> =
        repository.getSessionPoints(sessionId)
}
