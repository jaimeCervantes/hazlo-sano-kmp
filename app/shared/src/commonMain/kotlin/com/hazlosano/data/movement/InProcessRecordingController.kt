package com.hazlosano.data.movement

import com.hazlosano.data.currentEpochMilliseconds
import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.repository.RecordingController
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.time.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

/**
 * Records in the app's own process. Used by targets without a background mechanism of their own
 * (desktop, web, iOS today), where a recording lasts as long as the app runs.
 */
class InProcessRecordingController(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : RecordingController {

    private val recording = SessionRecording(
        scope = scope,
        locationRepository = createLocationRepository(),
        saveSession = SaveSessionUseCase(
            movementSessionRepository(),
            TimeProvider { currentEpochMilliseconds() },
        ),
        timeProvider = TimeProvider { currentEpochMilliseconds() },
    )

    override val state: StateFlow<RecordingState> = recording.state
    override val lastSavedSession: StateFlow<RecordingState?> = recording.lastSavedSession

    override fun startRecording(captureTrace: Boolean) = recording.start(captureTrace)

    override fun stopRecording() {
        recording.stop()
    }

    override fun acknowledgeSavedSession() = recording.acknowledgeSavedSession()
}
