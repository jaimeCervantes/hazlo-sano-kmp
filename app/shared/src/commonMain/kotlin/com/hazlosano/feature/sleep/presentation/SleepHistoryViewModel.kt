package com.hazlosano.feature.sleep.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.model.SleepHistory
import com.hazlosano.domain.usecase.GetSleepHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.hazlosano.data.currentEpochMilliseconds

class SleepHistoryViewModel(
    private val getSleepHistoryUseCase: GetSleepHistoryUseCase,
    private val days: Int = 7,
) : ViewModel() {

    private val _state = MutableStateFlow<SleepHistoryUiState>(SleepHistoryUiState.Loading)
    val state: StateFlow<SleepHistoryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        viewModelScope.launch { loadData() }
    }

    private fun load() {
        viewModelScope.launch { loadData() }
    }

    private suspend fun loadData() {
        try {
            val now = currentEpochMilliseconds()
            val from = now - days * 24 * 60 * 60 * 1000L
            val history = getSleepHistoryUseCase(from, to = now)
            _state.value = SleepHistoryUiState.Success(history)
        } catch (e: Exception) {
            _state.value = SleepHistoryUiState.Error
        }
    }
}

sealed interface SleepHistoryUiState {
    data object Loading : SleepHistoryUiState
    data class Success(val history: SleepHistory) : SleepHistoryUiState
    /**
     * No se pudo leer. Sin mensaje: el de la excepción viene de la base y estaba cayendo en un
     * "Unknown error" en inglés que se le enseñaba a una persona que usa el app en español.
     */
    data object Error : SleepHistoryUiState
}
