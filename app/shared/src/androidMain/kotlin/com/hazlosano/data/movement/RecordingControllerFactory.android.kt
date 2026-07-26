package com.hazlosano.data.movement

import android.content.Context
import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.repository.RecordingController
import kotlinx.coroutines.flow.StateFlow

/**
 * Hands the recording to [MovementRecordingService] so it survives leaving the tracker screen, and
 * observes what the service records through [MovementRecordingStore].
 */
private class ServiceRecordingController(
    private val context: Context,
) : RecordingController {

    override val state: StateFlow<RecordingState> = MovementRecordingStore.state
    override val lastSavedSession: StateFlow<RecordingState?> = MovementRecordingStore.lastSavedSession

    override fun startRecording(captureTrace: Boolean) {
        MovementRecordingService.start(context, captureTrace)
    }

    override fun stopRecording() {
        MovementRecordingService.stop(context)
    }

    override fun acknowledgeSavedSession() {
        MovementRecordingStore.publishSavedSession(null)
    }
}

actual fun createRecordingController(): RecordingController =
    ServiceRecordingController(MovementServiceLocator.requireContext())
