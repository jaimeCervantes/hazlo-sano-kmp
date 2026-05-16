package com.hazlosano.kmp.feature.sleep.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.kmp.domain.model.SleepHistory
import com.hazlosano.kmp.domain.usecase.GetSleepHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.hazlosano.kmp.data.currentEpochMilliseconds

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
            _state.value = SleepHistoryUiState.Error(e.message ?: "Unknown error")
        }
    }
}

sealed interface SleepHistoryUiState {
    data object Loading : SleepHistoryUiState
    data class Success(val history: SleepHistory) : SleepHistoryUiState
    data class Error(val message: String) : SleepHistoryUiState
}
