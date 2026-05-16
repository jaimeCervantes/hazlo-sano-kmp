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
import com.hazlosano.kmp.data.currentEpochMilliseconds
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

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
                    val (from, to) = analysisWindow()
                    getSleepAnalysisUseCase(from, to)
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
            val (from, to) = analysisWindow()
            val analysis = getSleepAnalysisUseCase(from, to)
            _uiState.value = state.copy(sleepAnalysis = analysis)
            loadSessions(from, to)
        } catch (_: Exception) { }
    }

    private suspend fun loadSessions(from: Long, to: Long) {
        sleepSessionRepository?.let { repo ->
            _sessions.value = repo.getSleepSessions(from, to)
        }
    }

    companion object {
        private const val NIGHT_BOUNDARY_HOUR = 18

        fun analysisWindow(): Pair<Long, Long> {
            val tz = TimeZone.currentSystemDefault()
            val now = Instant.fromEpochMilliseconds(currentEpochMilliseconds())
            val local = now.toLocalDateTime(tz)
            // Always start from yesterday 6 PM local — captures last night's sleep
            val yesterday = local.date.minus(1, DateTimeUnit.DAY)
            val from = yesterday.atStartOfDayIn(tz).toEpochMilliseconds() +
                NIGHT_BOUNDARY_HOUR * 3600_000L
            return from to now.toEpochMilliseconds()
        }
    }
}
