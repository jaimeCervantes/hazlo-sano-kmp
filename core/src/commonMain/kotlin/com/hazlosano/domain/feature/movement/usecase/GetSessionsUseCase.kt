package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.Flow

class GetSessionsUseCase(
    private val repository: RouteRepository
) {
    operator fun invoke(): Flow<List<MovementSession>> = repository.getAllSessions()

    fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> {
        return repository.getSessionPoints(sessionId)
    }
}
