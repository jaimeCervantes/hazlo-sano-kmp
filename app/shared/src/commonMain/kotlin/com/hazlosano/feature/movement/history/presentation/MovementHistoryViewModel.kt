package com.hazlosano.feature.movement.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.usecase.GetSessionsUseCase
import com.hazlosano.feature.movement.presentation.MovementFormat
import com.hazlosano.feature.movement.presentation.TrackSilhouette
import com.hazlosano.feature.movement.presentation.toSilhouette
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
    /**
     * La forma del recorrido, ya proyectada. Vacía en las salidas grabadas antes de que se guardara
     * la silueta: la lista enseña su hueco en vez de inventarles un trazado.
     */
    val silhouette: TrackSilhouette,
)

sealed interface MovementHistoryUiState {
    data object Loading : MovementHistoryUiState
    data object Empty : MovementHistoryUiState
    data class Sessions(val items: List<SessionListItem>) : MovementHistoryUiState
    /**
     * No se pudo leer el historial. Sin mensaje: el de la excepción viene de la base, está en
     * inglés y no le dice nada a nadie. La UI pone la frase, que es donde hay catálogo.
     */
    data object Error : MovementHistoryUiState
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
                .catch { _ -> _state.value = MovementHistoryUiState.Error }
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
            silhouette = previewPoints.toSilhouette(),
        )
}
