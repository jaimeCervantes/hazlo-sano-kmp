package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.repository.RecordingController

/**
 * Creates the platform [RecordingController]. Android hands the recording to a foreground service so
 * it survives leaving the app; other targets record in process.
 */
expect fun createRecordingController(): RecordingController
