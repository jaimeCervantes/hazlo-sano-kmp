package com.hazlosano.data.movement

import com.hazlosano.data.movement.trace.TraceSink
import com.hazlosano.data.movement.trace.TraceStore
import com.hazlosano.data.movement.trace.createTraceStore
import com.hazlosano.domain.feature.movement.filter.LocationFilter
import com.hazlosano.domain.feature.movement.filter.LocationFilterResult
import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.model.elapsedAt
import com.hazlosano.domain.feature.movement.model.observed
import com.hazlosano.domain.feature.movement.model.recorded
import com.hazlosano.domain.feature.movement.model.started
import com.hazlosano.domain.feature.movement.model.stopped
import com.hazlosano.domain.feature.movement.repository.LocationRepository
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
     * Owned by the collecting job and closed out by [stop]. Like [state], it assumes the recording
     * is driven from one thread — the host's — which is how every caller uses it.
     */
    private var filter = LocationFilter()

    /**
     * La ruta que la salida en curso va siguiendo, si alguna. Se guarda al arrancar porque al cerrar
     * ya no hay de dónde sacarla, y es lo que hace que la sesión recuerde con qué salió.
     */
    private var followedRouteId: Long? = null

    /**
     * [captureTrace] keeps every reading the receiver delivers, with the filter's verdict, for
     * calibrating the thresholds against a real GPS. It is decided per recording and defaults to
     * off: an app in normal use has no business writing a file for every session.
     */
    fun start(captureTrace: Boolean = false, routeId: Long? = null) {
        if (_state.value.isRecording) return
        _lastSavedSession.value = null
        followedRouteId = routeId
        val startedAtMillis = timeProvider.nowMillis()
        _state.value = RecordingState().started(startedAtMillis)

        val sink = if (captureTrace) traceStore.openTrace(startedAtMillis) else null
        traceSink = sink

        // Started fresh, so a session begins without any memory of the previous one instead of
        // inheriting its estimate. It is read again in stop(), to close out the departure the
        // filter may still be holding.
        filter = LocationFilter()
        locationJob = scope.launch {
            locationRepository.getLocationUpdates().collect { location ->
                // Noted whatever becomes of it: a reading the filter turns away is still the
                // receiver reporting, and the difference between that and silence is what tells a
                // phone left on a table from a phone that lost its signal.
                _state.value = _state.value.observed(location.timestamp)
                val outcome = filter.accepting(location)
                filter = outcome.filter
                // Written raw, and only once the filter has settled the verdict: a departure is
                // judged well after it starts, so writing what a reading looked like on arrival
                // would report readings as unconfirmed that ended up in the path. The rejected half
                // is what a saved session cannot tell you about, and what makes the trace
                // replayable against a different threshold.
                outcome.settled.forEach { sink?.append(it) }
                when (outcome) {
                    // The corrected positions are what get recorded, so the drawn path, the
                    // distance and the statistics recomputed from the points all describe the same
                    // journey. Normally one; the whole held departure when this reading is the one
                    // that confirmed it.
                    is LocationFilterResult.Accepted -> {
                        outcome.locations.forEach { _state.value = _state.value.recorded(it) }
                    }
                    // Noise, a jump, a fix too vague to be worth metres, or a departure that has
                    // not held up yet: the path does not grow.
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
        val stoppedAtMillis = timeProvider.nowMillis()
        locationJob?.cancel()
        locationJob = null
        tickJob?.cancel()
        tickJob = null
        val sink = traceSink
        traceSink = null

        // The departure the filter was still holding is judged now rather than dropped: someone who
        // presses stop while still walking would otherwise lose the end of their outing.
        val closing = filter.closing()
        closing.settled.forEach { sink?.append(it) }
        var recorded = _state.value
        closing.released.forEach { recorded = recorded.recorded(it) }
        val finished = recorded.stopped(stoppedAtMillis)

        // A finished session belongs to the history, not to the tracker: clear it so the screen
        // stops drawing a route that is no longer being recorded.
        _state.value = RecordingState()
        if (finished.traveledPoints.isEmpty() && sink == null) return null

        return scope.launch {
            // The write must survive the host being torn down right after stopping.
            withContext(NonCancellable) {
                if (finished.traveledPoints.isNotEmpty()) {
                    // No statistics are computed here: everything the route shows is derived when
                    // the session is opened, so writing it now would only freeze it at today's
                    // algorithm and leave this session stale after the next improvement.
                    saveSession(
                        name = sessionName,
                        routeId = followedRouteId,
                        points = finished.traveledPoints,
                        elapsedSeconds = finished.elapsedSeconds,
                        distanceMeters = finished.distanceMeters,
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
