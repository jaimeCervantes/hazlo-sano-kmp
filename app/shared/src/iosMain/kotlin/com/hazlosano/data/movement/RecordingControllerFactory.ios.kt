package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.repository.RecordingController

actual fun createRecordingController(): RecordingController = InProcessRecordingController()
