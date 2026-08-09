package com.hazlosano.feature.movement.detail.presentation

import com.hazlosano.data.db.DatabaseProvider
import com.hazlosano.data.movement.movementSessionRepository
import com.hazlosano.data.movement.routeRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionDetailUseCase
import com.hazlosano.domain.feature.movement.usecase.RefreshSessionSummaryUseCase
import com.hazlosano.domain.feature.movement.usecase.SaveRouteFromSessionUseCase

/** Assembles a [SessionDetailViewModel] with its platform dependencies (manual DI). */
fun createSessionDetailViewModel(sessionId: Long): SessionDetailViewModel {
    val repository = movementSessionRepository()
    return SessionDetailViewModel(
        sessionId = sessionId,
        getSessionDetail = GetSessionDetailUseCase(repository),
        refreshSessionSummary = RefreshSessionSummaryUseCase(repository),
        // Offered only where a route can actually be kept. Without a database the save would
        // report success and store nothing, which is worse than not offering it.
        saveRouteFromSession = if (DatabaseProvider.isInitialized) {
            SaveRouteFromSessionUseCase(routes = routeRepository(), sessions = repository)
        } else {
            null
        },
    )
}
