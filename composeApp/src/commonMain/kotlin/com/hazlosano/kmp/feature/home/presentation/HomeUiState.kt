package com.hazlosano.kmp.feature.home.presentation

import com.hazlosano.kmp.domain.model.HomeContent

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val content: HomeContent) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
