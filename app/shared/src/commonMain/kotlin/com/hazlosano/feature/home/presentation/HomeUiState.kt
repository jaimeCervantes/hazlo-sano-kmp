package com.hazlosano.feature.home.presentation

import com.hazlosano.domain.model.HomeContent

/**
 * En qué estado está Inicio.
 *
 * `Failed` no lleva texto: la redacción vive en el catálogo de recursos, que es lo único que una
 * traducción alcanza. Antes viajaba aquí el `message` de la excepción, que además de intraducible
 * era lo que menos le importa a quien lo lee.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val content: HomeContent) : HomeUiState
    data object Failed : HomeUiState
}
