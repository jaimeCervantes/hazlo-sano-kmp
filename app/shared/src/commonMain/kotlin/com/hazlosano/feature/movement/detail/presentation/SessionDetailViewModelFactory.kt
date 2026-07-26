package com.hazlosano.feature.movement.detail.presentation

import com.hazlosano.data.movement.movementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionDetailUseCase
import com.hazlosano.domain.feature.movement.usecase.RefreshSessionSummaryUseCase

/** Assembles a [SessionDetailViewModel] with its platform dependencies (manual DI). */
fun createSessionDetailViewModel(sessionId: Long): SessionDetailViewModel {
    val repository = movementSessionRepository()
    return SessionDetailViewModel(
        sessionId = sessionId,
        getSessionDetail = GetSessionDetailUseCase(repository),
        refreshSessionSummary = RefreshSessionSummaryUseCase(repository),
    )
}
