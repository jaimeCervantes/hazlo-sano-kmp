package com.hazlosano.feature.movement.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.data.movement.trace.TraceStore
import com.hazlosano.data.movement.trace.createTraceStore
import com.hazlosano.domain.feature.movement.filter.summarize
import com.hazlosano.domain.feature.movement.model.SessionDetail
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.usecase.GetSessionDetailUseCase
import com.hazlosano.feature.movement.presentation.MovementFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/** A recorded session as the detail renders it: display labels plus the path to draw. */
data class SessionDetailUi(
    val name: String,
    val dateLabel: String,
    val distanceLabel: String,
    val durationLabel: String,
    val paceLabel: String,
    val elevationLabel: String,
    val path: List<UserLocation>,
    /** Null when this session was recorded without the trace capture, so there is nothing to say. */
    val diagnosis: SessionDiagnosisUi? = null,
) {
    val hasPath: Boolean
        get() = path.size >= 2
}

sealed interface SessionDetailUiState {
    data object Loading : SessionDetailUiState
    data object Missing : SessionDetailUiState
    data class Detail(val session: SessionDetailUi) : SessionDetailUiState
    data class Error(val message: String) : SessionDetailUiState
}

/**
 * Streams one recorded session with its path and maps it to display labels. Formatting stays here
 * (not in the Composable) so the detail is unit-testable and reads the same as the history.
 */
class SessionDetailViewModel(
    private val sessionId: Long,
    private val getSessionDetail: GetSessionDetailUseCase,
    private val traceStore: TraceStore = createTraceStore(),
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<SessionDetailUiState>(SessionDetailUiState.Loading)
    val state: StateFlow<SessionDetailUiState> = _state.asStateFlow()

    init {
        observeSessionDetail()
    }

    private fun observeSessionDetail() {
        viewModelScope.launch {
            getSessionDetail(sessionId)
                .catch { failure ->
                    _state.value = SessionDetailUiState.Error(
                        failure.message ?: "No se pudo leer la sesión",
                    )
                }
                .collect { detail ->
                    _state.value = detail?.toUiState() ?: SessionDetailUiState.Missing
                }
        }
    }

    private suspend fun SessionDetail.toUiState(): SessionDetailUiState =
        SessionDetailUiState.Detail(
            SessionDetailUi(
                name = session.name,
                dateLabel = MovementFormat.dateTime(session.date, timeZone),
                distanceLabel = MovementFormat.distance(session.distanceTraveled),
                durationLabel = MovementFormat.duration(session.elapsedTime),
                paceLabel = MovementFormat.pace(session.avgPaceMinKm),
                elevationLabel = MovementFormat.elevation(session.elevationGain),
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
