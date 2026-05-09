package com.hazlosano.kmp.feature.sleep.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.kmp.domain.usecase.GetSleepAnalysisUseCase
import com.hazlosano.kmp.domain.usecase.GetSleepContentUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SleepViewModel(
    private val getSleepContentUseCase: GetSleepContentUseCase,
    private val getSleepAnalysisUseCase: GetSleepAnalysisUseCase? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SleepUiState>(SleepUiState.Loading)
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val content = async { getSleepContentUseCase() }
                val analysis = async {
                    val now = System.currentTimeMillis()
                    getSleepAnalysisUseCase?.invoke(now - 24 * 60 * 60 * 1000, now)
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
}
