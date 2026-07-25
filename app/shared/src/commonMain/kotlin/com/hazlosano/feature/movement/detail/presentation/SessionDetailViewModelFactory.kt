package com.hazlosano.feature.movement.detail.presentation

import com.hazlosano.data.movement.movementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionDetailUseCase

/** Assembles a [SessionDetailViewModel] with its platform dependencies (manual DI). */
fun createSessionDetailViewModel(sessionId: Long): SessionDetailViewModel =
    SessionDetailViewModel(
        sessionId = sessionId,
        getSessionDetail = GetSessionDetailUseCase(movementSessionRepository()),
    )
