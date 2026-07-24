package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.data.currentEpochMilliseconds
import com.hazlosano.data.db.DatabaseProvider
import com.hazlosano.data.movement.NoOpMovementSessionRepository
import com.hazlosano.data.movement.SqlDelightMovementSessionRepository
import com.hazlosano.data.movement.createLocationRepository
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.time.TimeProvider

/** Assembles a [TrackerViewModel] with its platform dependencies (manual DI, as elsewhere in App). */
fun createTrackerViewModel(): TrackerViewModel {
    val sessionRepository: MovementSessionRepository =
        if (DatabaseProvider.isInitialized) {
            SqlDelightMovementSessionRepository(DatabaseProvider.get())
        } else {
            NoOpMovementSessionRepository
        }
    return TrackerViewModel(
        locationRepository = createLocationRepository(),
        saveSession = SaveSessionUseCase(sessionRepository, TimeProvider { currentEpochMilliseconds() }),
    )
}
