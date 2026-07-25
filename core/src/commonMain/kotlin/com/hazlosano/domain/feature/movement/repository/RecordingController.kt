package com.hazlosano.domain.feature.movement.repository

import com.hazlosano.domain.feature.movement.model.RecordingState
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the session being recorded, wherever it actually runs: on Android a foreground service keeps
 * it alive outside the app, while other targets record in process. The tracker screen observes this
 * instead of holding the recording itself, so leaving the screen cannot end a session.
 */
interface RecordingController {
    /** The session in progress. Goes back to empty once a finished session has been saved. */
    val state: StateFlow<RecordingState>

    /**
     * The session that was just saved, for the screen to confirm it, or null when there is nothing
     * pending to confirm. It is not part of [state] because a finished session no longer belongs to
     * the tracker: it belongs to the history.
     */
    val lastSavedSession: StateFlow<RecordingState?>

    fun startRecording()
    fun stopRecording()

    /** Drops the confirmation once the screen has shown it. */
    fun acknowledgeSavedSession()
}
