package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.data.movement.createLocationRepository
import com.hazlosano.data.movement.createRecordingController

/** Assembles a [TrackerViewModel] with its platform dependencies (manual DI, as elsewhere in App). */
fun createTrackerViewModel(): TrackerViewModel =
    TrackerViewModel(
        locationRepository = createLocationRepository(),
        recordingController = createRecordingController(),
    )
