package com.hazlosano.feature.movement.history.presentation

import com.hazlosano.data.movement.movementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionsUseCase

/** Assembles a [MovementHistoryViewModel] with its platform dependencies (manual DI). */
fun createMovementHistoryViewModel(): MovementHistoryViewModel =
    MovementHistoryViewModel(getSessions = GetSessionsUseCase(movementSessionRepository()))
