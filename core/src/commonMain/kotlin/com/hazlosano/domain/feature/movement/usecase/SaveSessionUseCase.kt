package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.time.TimeProvider
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.model.sampledForPreview
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository

/**
 * Persists a finished recording: the readings it accepted, plus the summary the history needs to
 * list it without reading any of them.
 *
 * No statistics are written. Everything the route shows is derived when the session is opened, so
 * writing it here would only freeze it at today's algorithm.
 */
class SaveSessionUseCase(
    private val repository: MovementSessionRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        name: String,
        routeId: Long?,
        points: List<UserLocation>,
        elapsedSeconds: Long,
        distanceMeters: Double,
    ) {
        val session = MovementSession(
            routeId = routeId,
            name = name,
            date = timeProvider.nowMillis(),
            elapsedTime = elapsedSeconds,
            distanceTraveled = distanceMeters,
            previewPoints = points.sampledForPreview(),
        )
        repository.saveSession(session, points)
    }
}
