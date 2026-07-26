package com.hazlosano.data.movement

import com.hazlosano.data.movement.trace.TraceSink
import com.hazlosano.data.movement.trace.TraceStore
import com.hazlosano.data.movement.trace.createTraceStore
import com.hazlosano.domain.feature.movement.filter.LocationFilter
import com.hazlosano.domain.feature.movement.filter.LocationFilterResult
import com.hazlosano.domain.feature.movement.filter.TraceRecord
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
    private val traceStore: TraceStore = createTraceStore(),
) {
    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _lastSavedSession = MutableStateFlow<RecordingState?>(null)
    val lastSavedSession: StateFlow<RecordingState?> = _lastSavedSession.asStateFlow()

    private var locationJob: Job? = null
    private var tickJob: Job? = null
    private var traceSink: TraceSink? = null

    /**
     * [captureTrace] keeps every reading the receiver delivers, with the filter's verdict, for
     * calibrating the thresholds against a real GPS. It is decided per recording and defaults to
     * off: an app in normal use has no business writing a file for every session.
     */
    fun start(captureTrace: Boolean = false) {
        if (_state.value.isRecording) return
        _lastSavedSession.value = null
        val startedAtMillis = timeProvider.nowMillis()
        _state.value = RecordingState().started(startedAtMillis)

        val sink = if (captureTrace) traceStore.openTrace(startedAtMillis) else null
        traceSink = sink

        locationJob = scope.launch {
            // The filter is local to this job, so every session starts without any memory of the
            // previous one instead of inheriting its estimate.
            var filter = LocationFilter()
            locationRepository.getLocationUpdates().collect { location ->
                val outcome = filter.accepting(location)
                filter = outcome.filter
                // Recorded before acting on it, and recorded raw: the rejected readings are the
                // half that a saved session cannot tell you about, and they are what makes the
                // trace replayable against a different threshold.
                sink?.append(
                    TraceRecord(
                        reading = location,
                        discardReason = (outcome as? LocationFilterResult.Discarded)?.reason,
                    ),
                )
                when (outcome) {
                    // The corrected position is what gets recorded, so the drawn path, the distance
                    // and the statistics recomputed from the points all describe the same journey.
                    is LocationFilterResult.Accepted -> {
                        _state.value = _state.value.recorded(outcome.location)
                    }
                    // Noise, a jump or a fix too vague to be worth metres: the path does not grow.
                    is LocationFilterResult.Discarded -> Unit
                }
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
        val sink = traceSink
        traceSink = null

        // A finished session belongs to the history, not to the tracker: clear it so the screen
        // stops drawing a route that is no longer being recorded.
        _state.value = RecordingState()
        if (finished.traveledPoints.isEmpty() && sink == null) return null

        return scope.launch {
            // The write must survive the host being torn down right after stopping.
            withContext(NonCancellable) {
                if (finished.traveledPoints.isNotEmpty()) {
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
                // Closed last, and with the session's first stored point, so the detail can find
                // the trace that belongs to it.
                sink?.finish(finished.traveledPoints.firstOrNull()?.timestamp)
            }
        }
    }

    fun acknowledgeSavedSession() {
        _lastSavedSession.value = null
    }
}
