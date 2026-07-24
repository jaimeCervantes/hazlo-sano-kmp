package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.data.currentEpochMilliseconds
import com.hazlosano.data.movement.createLocationRepository
import com.hazlosano.data.movement.movementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.time.TimeProvider

/** Assembles a [TrackerViewModel] with its platform dependencies (manual DI, as elsewhere in App). */
fun createTrackerViewModel(): TrackerViewModel =
    TrackerViewModel(
        locationRepository = createLocationRepository(),
        saveSession = SaveSessionUseCase(
            movementSessionRepository(),
            TimeProvider { currentEpochMilliseconds() },
        ),
    )
