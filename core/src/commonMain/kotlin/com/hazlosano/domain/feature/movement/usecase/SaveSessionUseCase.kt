package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.time.TimeProvider
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.NavigationState
import com.hazlosano.domain.feature.movement.repository.RouteRepository

class SaveSessionUseCase(
    private val repository: RouteRepository,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(name: String, routeId: Long?, state: NavigationState) {
        // Simple downsampling for preview (e.g. max 200 points)
        val rawPoints = state.traveledPoints
        val step = if (rawPoints.size > 200) rawPoints.size / 200 else 1
        val preview = if (step > 1) rawPoints.filterIndexed { index, _ -> index % step == 0 } else rawPoints

        val session = MovementSession(
            routeId = routeId,
            name = name,
            date = timeProvider.nowMillis(),
            elapsedTime = state.elapsedTime,
            distanceTraveled = state.distanceTraveled,
            elevationGain = state.elevationGain,
            previewPoints = preview,
            stats = state.stats
        )
        repository.saveSession(session, rawPoints)
    }
}
