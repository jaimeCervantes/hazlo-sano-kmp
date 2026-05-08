package com.hazlosano.kmp.feature.sleep.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.kmp.domain.usecase.GetSleepContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SleepViewModel(
    private val getSleepContentUseCase: GetSleepContentUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SleepUiState>(SleepUiState.Loading)
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val content = getSleepContentUseCase()
                _uiState.value = SleepUiState.Success(content)
            } catch (e: Exception) {
                _uiState.value = SleepUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
