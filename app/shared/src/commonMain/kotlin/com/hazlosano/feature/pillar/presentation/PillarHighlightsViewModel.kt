package com.hazlosano.feature.pillar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.usecase.GetPillarHighlightsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Los campeones y retos de un pilar.
 *
 * Va aparte del ViewModel del catálogo aunque las dos cosas se pinten en la misma pantalla: no
 * dependen de la misma fuente ni tardan lo mismo. Tenerlos separados es lo que permite que una
 * espera de red no arrastre a lo que ya está listo.
 */
class PillarHighlightsViewModel(
    private val pillar: PillarType,
    private val getPillarHighlights: GetPillarHighlightsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PillarHighlightsUiState>(PillarHighlightsUiState.Loading)
    val uiState: StateFlow<PillarHighlightsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = PillarHighlightsUiState.Loading
            _uiState.value = runCatching { getPillarHighlights(pillar) }
                .fold(
                    onSuccess = { PillarHighlightsUiState.Ready(it) },
                    onFailure = { PillarHighlightsUiState.Unavailable },
                )
        }
    }
}
