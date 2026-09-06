package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.repository.RecordingController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stands in for the foreground service (or its in-process equivalent).
 *
 * Vive en su propio archivo desde que un segundo test lo necesito: dos dobles de la misma interfaz
 * en el mismo paquete era una redeclaracion, y ademas la copia habria envejecido aparte.
 */
internal class FakeRecordingController : RecordingController {
    private val _state = MutableStateFlow(RecordingState())
    override val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _lastSavedSession = MutableStateFlow<RecordingState?>(null)
    override val lastSavedSession: StateFlow<RecordingState?> = _lastSavedSession.asStateFlow()

    var startCount: Int = 0
        private set
    var stopCount: Int = 0
        private set
    var startedWithTraceCapture: Boolean? = null
        private set

    /** Con que ruta arranco cada grabacion, en orden. `null` es salir sin ninguna. */
    val startedWithRouteId: MutableList<Long?> = mutableListOf()

    override fun startRecording(captureTrace: Boolean, routeId: Long?) {
        startCount++
        startedWithTraceCapture = captureTrace
        startedWithRouteId += routeId
        _lastSavedSession.value = null
        _state.value = RecordingState(isRecording = true, startedAtMillis = 0)
    }

    override fun stopRecording() {
        stopCount++
        _lastSavedSession.value = _state.value.copy(isRecording = false)
        _state.value = RecordingState()
    }

    override fun acknowledgeSavedSession() {
        _lastSavedSession.value = null
    }

    /** Simulates what the service records while the screen is elsewhere. */
    fun publish(state: RecordingState) {
        _state.value = state
    }
}
