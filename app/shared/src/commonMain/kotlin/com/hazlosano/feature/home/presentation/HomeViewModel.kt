package com.hazlosano.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.usecase.GetHomeContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getHomeContentUseCase: GetHomeContentUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    /** Reintenta tras un fallo. Es la misma carga que corre al abrir la pantalla. */
    fun refresh() {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _uiState.value = runCatching { getHomeContentUseCase() }
                .fold(
                    onSuccess = { HomeUiState.Success(it) },
                    onFailure = { HomeUiState.Failed },
                )
        }
    }
}
