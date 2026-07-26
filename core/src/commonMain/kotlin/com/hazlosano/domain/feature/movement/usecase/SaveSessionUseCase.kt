package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.time.TimeProvider
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository

private const val MAX_PREVIEW_POINTS = 200

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
        val step = if (points.size > MAX_PREVIEW_POINTS) points.size / MAX_PREVIEW_POINTS else 1
        val preview = if (step > 1) points.filterIndexed { index, _ -> index % step == 0 } else points

        val session = MovementSession(
            routeId = routeId,
            name = name,
            date = timeProvider.nowMillis(),
            elapsedTime = elapsedSeconds,
            distanceTraveled = distanceMeters,
            previewPoints = preview,
        )
        repository.saveSession(session, points)
    }
}
