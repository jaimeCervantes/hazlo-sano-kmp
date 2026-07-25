package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.SessionDetail
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Streams one recorded session with its stored path, or null when that session no longer exists.
 *
 * The session is picked from the stored sessions instead of a dedicated query: the repository
 * contract stays as narrow as it is today, and a user's session list is small enough that the
 * lookup is not worth a schema change.
 */
class GetSessionDetailUseCase(
    private val repository: MovementSessionRepository,
) {
    operator fun invoke(sessionId: Long): Flow<SessionDetail?> =
        combine(
            repository.getAllSessions(),
            repository.getSessionPoints(sessionId),
        ) { sessions, path ->
            sessions.firstOrNull { it.id == sessionId }?.let { session ->
                SessionDetail(session = session, path = path)
            }
        }
}
