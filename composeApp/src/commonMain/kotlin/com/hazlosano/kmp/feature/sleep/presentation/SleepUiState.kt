package com.hazlosano.kmp.feature.sleep.presentation

import com.hazlosano.kmp.domain.model.SleepAnalysis
import com.hazlosano.kmp.domain.model.SleepContent

sealed interface SleepUiState {
    data object Loading : SleepUiState
    data class Success(
        val content: SleepContent,
        val sleepAnalysis: SleepAnalysis? = null,
    ) : SleepUiState
    data class Error(val message: String) : SleepUiState
}
