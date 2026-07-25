package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.model.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The recording in progress, published so the UI can observe what the foreground service records.
 *
 * It is process-wide on purpose: the service outlives every screen, and the alternative — binding to
 * the service from each Composable — buys nothing here and adds connection state to get wrong. Scope
 * is deliberately narrow: the current recording, written by [MovementRecordingService], plus the
 * pending confirmation of the last saved session, which the screen clears once it has shown it.
 */
internal object MovementRecordingStore {

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _lastSavedSession = MutableStateFlow<RecordingState?>(null)
    val lastSavedSession: StateFlow<RecordingState?> = _lastSavedSession.asStateFlow()

    fun publish(state: RecordingState) {
        _state.value = state
    }

    fun publishSavedSession(session: RecordingState?) {
        _lastSavedSession.value = session
    }
}
