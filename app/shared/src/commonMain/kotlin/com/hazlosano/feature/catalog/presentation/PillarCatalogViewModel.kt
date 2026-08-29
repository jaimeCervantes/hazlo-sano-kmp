package com.hazlosano.feature.catalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.usecase.GetPillarCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * El catálogo de un pilar.
 *
 * La ubicación llega por función y no por constructor porque leerla es una suspensión que puede dar
 * `null`, y porque cambia entre una apertura y la siguiente: fijarla al construir la congelaría en
 * donde estaba el teléfono la primera vez.
 */
class PillarCatalogViewModel(
    private val pillar: PillarType,
    private val getPillarCatalog: GetPillarCatalogUseCase,
    private val readLocation: suspend () -> VisitorLocation?,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PillarCatalogUiState>(PillarCatalogUiState.Loading)
    val uiState: StateFlow<PillarCatalogUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = PillarCatalogUiState.Loading
            // Que no se pueda leer la ubicación no cancela la lectura del catálogo: sale ordenado
            // por fecha, que es lo que el sitio hace con quien no dice dónde está.
            val location = runCatching { readLocation() }.getOrNull()

            _uiState.value = runCatching { getPillarCatalog(pillar, location) }
                .fold(
                    onSuccess = { it.toUiState() },
                    onFailure = { PillarCatalogUiState.Failed },
                )
        }
    }

    private fun CatalogPage.toUiState(): PillarCatalogUiState = when (this) {
        is CatalogPage.Fresh -> PillarCatalogUiState.Ready(publications, fromCache = false)
        is CatalogPage.Cached -> PillarCatalogUiState.Ready(publications, fromCache = true)
        CatalogPage.Unavailable -> PillarCatalogUiState.Unavailable
    }
}
