package com.hazlosano.feature.movement.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.data.movement.trace.TraceStore
import com.hazlosano.data.movement.trace.createTraceStore
import com.hazlosano.domain.feature.movement.filter.summarize
import com.hazlosano.domain.feature.movement.model.SessionDetail
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.usecase.GetSessionDetailUseCase
import com.hazlosano.domain.feature.movement.usecase.RefreshSessionSummaryUseCase
import com.hazlosano.domain.feature.movement.usecase.SaveRouteFromSessionUseCase
import com.hazlosano.feature.movement.presentation.MovementFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * De qué cifra de la sesión habla una fila del resumen.
 *
 * Cerrado y no un `String` porque el rótulo es copia: escrito aquí no lo alcanza ningún catálogo,
 * y además obligaba a las pruebas a buscar la fila por su redacción en español.
 */
enum class SessionMetric {
    DISTANCE,
    DURATION,
    MOVING_TIME,
    PACE,
    ASCENT,
    DESCENT,
    MAX_ALTITUDE,
    MIN_ALTITUDE,
}

/** One figure of a finished session, already formatted. "—" where nothing was measured. */
data class SessionMetricUi(val metric: SessionMetric, val value: String)

/** A recorded session as the detail renders it: display labels plus the path to draw. */
data class SessionDetailUi(
    val name: String,
    val dateLabel: String,
    val distanceLabel: String,
    val durationLabel: String,
    val movingTimeLabel: String,
    val paceLabel: String,
    val ascentLabel: String,
    val descentLabel: String,
    val maxAltitudeLabel: String,
    val minAltitudeLabel: String,
    val path: List<UserLocation>,
    /** Null when this session was recorded without the trace capture, so there is nothing to say. */
    val diagnosis: SessionDiagnosisUi? = null,
) {
    val hasPath: Boolean
        get() = path.size >= 2

    /** In the order the summary reads them, so the screen only has to lay them out. */
    val metrics: List<SessionMetricUi>
        get() = listOf(
            SessionMetricUi(SessionMetric.DISTANCE, distanceLabel),
            SessionMetricUi(SessionMetric.DURATION, durationLabel),
            SessionMetricUi(SessionMetric.MOVING_TIME, movingTimeLabel),
            SessionMetricUi(SessionMetric.PACE, paceLabel),
            SessionMetricUi(SessionMetric.ASCENT, ascentLabel),
            SessionMetricUi(SessionMetric.DESCENT, descentLabel),
            SessionMetricUi(SessionMetric.MAX_ALTITUDE, maxAltitudeLabel),
            SessionMetricUi(SessionMetric.MIN_ALTITUDE, minAltitudeLabel),
        )
}

sealed interface SessionDetailUiState {
    data object Loading : SessionDetailUiState
    data object Missing : SessionDetailUiState
    data class Detail(val session: SessionDetailUi) : SessionDetailUiState
    /**
     * No se pudo leer. Sin mensaje: el que traen las excepciones viene de la base o del parser,
     * está en inglés y habla de detalles que no ayudan a nadie. La UI dice lo suyo.
     */
    data object Error : SessionDetailUiState
}

/**
 * Streams one recorded session with its path and maps it to display labels.
 *
 * The figures come from the route every time the session is opened, so improving how the app
 * measures improves the outings already recorded. Opening a session also refreshes the summary the
 * history lists it by, which is what keeps the two screens from disagreeing.
 *
 * Formatting stays here rather than in the Composable so the detail is unit-testable and reads the
 * same as the history.
 */
class SessionDetailViewModel(
    private val sessionId: Long,
    private val getSessionDetail: GetSessionDetailUseCase,
    private val refreshSessionSummary: RefreshSessionSummaryUseCase,
    private val saveRouteFromSession: SaveRouteFromSessionUseCase? = null,
    private val traceStore: TraceStore = createTraceStore(),
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<SessionDetailUiState>(SessionDetailUiState.Loading)
    val state: StateFlow<SessionDetailUiState> = _state.asStateFlow()

    /** What saving this outing as a route had to say, once. Null when nothing has been said. */
    private val _saveRouteMessage = MutableStateFlow<SaveRouteMessage?>(null)
    val saveRouteMessage: StateFlow<SaveRouteMessage?> = _saveRouteMessage.asStateFlow()

    /** Hidden where routes cannot be stored at all, rather than failing when pressed. */
    val canSaveAsRoute: Boolean = saveRouteFromSession != null

    init {
        observeSessionDetail()
    }

    /**
     * Keeps this outing as a route to follow again, under a name the person chooses. The name is
     * asked for rather than taken from the session: "Salida del 9 ago" is what happened that day,
     * not what the route is called.
     */
    fun saveAsRoute(name: String) {
        val useCase = saveRouteFromSession ?: return
        viewModelScope.launch {
            _saveRouteMessage.value = when (val result = useCase(sessionId, name)) {
                is SaveRouteFromSessionUseCase.Result.Success ->
                    SaveRouteMessage.Saved(result.route.name)
                is SaveRouteFromSessionUseCase.Result.Failed ->
                    SaveRouteMessage.Failed(result.problem)
            }
        }
    }

    fun consumeSaveRouteMessage() {
        _saveRouteMessage.value = null
    }

    private fun observeSessionDetail() {
        viewModelScope.launch {
            getSessionDetail(sessionId)
                .catch { _ -> _state.value = SessionDetailUiState.Error }
                .collect { detail ->
                    _state.value = detail?.toUiState() ?: SessionDetailUiState.Missing
                    if (detail != null) refreshSessionSummary(detail)
                }
        }
    }

    private suspend fun SessionDetail.toUiState(): SessionDetailUiState =
        SessionDetailUiState.Detail(
            SessionDetailUi(
                name = session.name,
                dateLabel = MovementFormat.dateTime(session.date, timeZone),
                // Measured from the route now, not read from what was stored when it was recorded.
                distanceLabel = MovementFormat.distance(distanceMeters.takeIf { path.isNotEmpty() }),
                durationLabel = MovementFormat.duration(session.elapsedTime),
                movingTimeLabel = MovementFormat.duration(stats.movingTime),
                paceLabel = MovementFormat.pace(stats.avgPace),
                ascentLabel = MovementFormat.elevation(stats.totalAscent),
                descentLabel = MovementFormat.elevation(stats.totalDescent),
                maxAltitudeLabel = MovementFormat.elevation(stats.maxAltitude),
                minAltitudeLabel = MovementFormat.elevation(stats.minAltitude),
                path = path,
                diagnosis = readDiagnosis(),
            ),
        )

    /**
     * A session finds its own trace by the timestamp of its first stored point, which is the same
     * reading the filter first accepted. No session recorded without the capture has one, and that
     * is reported as no diagnosis at all rather than as counts of zero.
     */
    private suspend fun SessionDetail.readDiagnosis(): SessionDiagnosisUi? {
        val firstPoint = path.firstOrNull() ?: return null
        return traceStore.readTrace(firstPoint.timestamp)?.summarize()?.toDiagnosisUi()
    }
}
