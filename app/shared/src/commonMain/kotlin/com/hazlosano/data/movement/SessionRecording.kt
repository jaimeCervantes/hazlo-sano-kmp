package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.model.NavigationState
import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.model.elapsedAt
import com.hazlosano.domain.feature.movement.model.recorded
import com.hazlosano.domain.feature.movement.model.started
import com.hazlosano.domain.feature.movement.model.stopped
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import com.hazlosano.domain.feature.movement.usecase.CalculateStatsUseCase
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.time.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** How often the elapsed time is refreshed for the UI; the value itself comes from the clock. */
private const val TICK_MILLIS = 1_000L

/**
 * Records a session: collects locations, keeps the [RecordingState] up to date and persists the
 * session when it stops. Runs wherever it is hosted — in the Android foreground service, or in
 * process on targets without one — so the recording rules live in one place.
 */
class SessionRecording(
    private val scope: CoroutineScope,
    private val locationRepository: LocationRepository,
    private val saveSession: SaveSessionUseCase,
    private val timeProvider: TimeProvider,
    private val calculateStats: CalculateStatsUseCase = CalculateStatsUseCase(),
    private val sessionName: String = "Sesión de movimiento",
) {
    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _lastSavedSession = MutableStateFlow<RecordingState?>(null)
    val lastSavedSession: StateFlow<RecordingState?> = _lastSavedSession.asStateFlow()

    private var locationJob: Job? = null
    private var tickJob: Job? = null

    fun start() {
        if (_state.value.isRecording) return
        _lastSavedSession.value = null
        _state.value = RecordingState().started(timeProvider.nowMillis())

        locationJob = scope.launch {
            locationRepository.getLocationUpdates().collect { location ->
                _state.value = _state.value.recorded(location)
            }
        }
        tickJob = scope.launch {
            while (_state.value.isRecording) {
                delay(TICK_MILLIS)
                _state.value = _state.value.elapsedAt(timeProvider.nowMillis())
            }
        }
    }

    /**
     * Ends the recording and persists it. Returns the job doing the saving so a host that shuts
     * itself down on stop — the Android service does — can wait for the write instead of killing it.
     */
    fun stop(): Job? {
        if (!_state.value.isRecording) return null
        val finished = _state.value.stopped(timeProvider.nowMillis())
        locationJob?.cancel()
        locationJob = null
        tickJob?.cancel()
        tickJob = null

        // A finished session belongs to the history, not to the tracker: clear it so the screen
        // stops drawing a route that is no longer being recorded.
        _state.value = RecordingState()
        if (finished.traveledPoints.isEmpty()) return null

        return scope.launch {
            // The write must survive the host being torn down right after stopping.
            withContext(NonCancellable) {
                val stats = calculateStats(finished.traveledPoints, finished.elapsedSeconds)
                saveSession(
                    name = sessionName,
                    routeId = null,
                    state = NavigationState(
                        traveledPoints = finished.traveledPoints,
                        elapsedTime = finished.elapsedSeconds,
                        distanceTraveled = finished.distanceMeters,
                        elevationGain = stats.totalAscent,
                        stats = stats,
                    ),
                )
                _lastSavedSession.value = finished
            }
        }
    }

    fun acknowledgeSavedSession() {
        _lastSavedSession.value = null
    }
}
