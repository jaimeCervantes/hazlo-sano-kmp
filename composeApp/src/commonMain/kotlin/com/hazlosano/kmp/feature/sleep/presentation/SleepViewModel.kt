package com.hazlosano.kmp.feature.sleep.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.repository.SleepSessionRepository
import com.hazlosano.kmp.domain.usecase.GetSleepAnalysisUseCase
import com.hazlosano.kmp.domain.usecase.GetSleepContentUseCase
import com.hazlosano.kmp.domain.model.SleepAnalysis
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SleepViewModel(
    private val getSleepContentUseCase: GetSleepContentUseCase,
    private val getSleepAnalysisUseCase: GetSleepAnalysisUseCase,
    private val sleepSessionRepository: SleepSessionRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SleepUiState>(SleepUiState.Loading)
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    private val _sessions = MutableStateFlow<List<SleepSession>>(emptyList())
    val sessions: StateFlow<List<SleepSession>> = _sessions.asStateFlow()

    val sleepAnalysis: StateFlow<SleepAnalysis?> = _uiState
        .map { state -> (state as? SleepUiState.Success)?.sleepAnalysis }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch { loadAnalysis() }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val content = async { getSleepContentUseCase() }
                val analysis = async {
                    val now = System.currentTimeMillis()
                    getSleepAnalysisUseCase(now - 24 * 60 * 60 * 1000, now)
                }
                _uiState.value = SleepUiState.Success(
                    content = content.await(),
                    sleepAnalysis = analysis.await(),
                )
            } catch (e: Exception) {
                _uiState.value = SleepUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun loadAnalysis() {
        val state = _uiState.value
        if (state !is SleepUiState.Success) return
        try {
            val now = System.currentTimeMillis()
            val analysis = getSleepAnalysisUseCase(now - 24 * 60 * 60 * 1000, now)
            _uiState.value = state.copy(sleepAnalysis = analysis)
            loadSessions(now)
        } catch (_: Exception) { }
    }

    private suspend fun loadSessions(now: Long) {
        sleepSessionRepository?.let { repo ->
            _sessions.value = repo.getSleepSessions(now - 24 * 60 * 60 * 1000, now)
        }
    }
}
