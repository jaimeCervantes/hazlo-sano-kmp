package com.hazlosano.feature.movement.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.usecase.GetSessionsUseCase
import com.hazlosano.feature.movement.presentation.MovementFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/** One recorded session as the history renders it: identity plus ready-to-display labels. */
data class SessionListItem(
    val id: Long,
    val name: String,
    val dateLabel: String,
    val distanceLabel: String,
    val durationLabel: String,
)

sealed interface MovementHistoryUiState {
    data object Loading : MovementHistoryUiState
    data object Empty : MovementHistoryUiState
    data class Sessions(val items: List<SessionListItem>) : MovementHistoryUiState
    data class Error(val message: String) : MovementHistoryUiState
}

/**
 * Streams the recorded sessions and maps them to display rows, newest first. Ordering and formatting
 * live here (not in the Composable or the query) so the history is correct regardless of the
 * repository that feeds it.
 */
class MovementHistoryViewModel(
    private val getSessions: GetSessionsUseCase,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<MovementHistoryUiState>(MovementHistoryUiState.Loading)
    val state: StateFlow<MovementHistoryUiState> = _state.asStateFlow()

    init {
        observeSessions()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            getSessions()
                .catch { failure ->
                    _state.value = MovementHistoryUiState.Error(
                        failure.message ?: "No se pudo leer el historial",
                    )
                }
                .collect { sessions -> _state.value = sessions.toUiState() }
        }
    }

    private fun List<MovementSession>.toUiState(): MovementHistoryUiState =
        if (isEmpty()) {
            MovementHistoryUiState.Empty
        } else {
            MovementHistoryUiState.Sessions(
                items = sortedByDescending { it.date }.map { it.toListItem() },
            )
        }

    private fun MovementSession.toListItem(): SessionListItem =
        SessionListItem(
            id = id,
            name = name,
            dateLabel = MovementFormat.dateTime(date, timeZone),
            distanceLabel = MovementFormat.distance(distanceTraveled),
            durationLabel = MovementFormat.duration(elapsedTime),
        )
}
