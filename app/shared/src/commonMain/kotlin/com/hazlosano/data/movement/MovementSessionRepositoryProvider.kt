package com.hazlosano.data.movement

import com.hazlosano.data.db.DatabaseProvider
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository

/**
 * Session persistence for the movement pillar: SQLDelight when the shared database is initialized
 * (Android today), otherwise a no-op so the tracker and its history still render on other targets.
 */
internal fun movementSessionRepository(): MovementSessionRepository =
    if (DatabaseProvider.isInitialized) {
        SqlDelightMovementSessionRepository(DatabaseProvider.get())
    } else {
        NoOpMovementSessionRepository
    }
