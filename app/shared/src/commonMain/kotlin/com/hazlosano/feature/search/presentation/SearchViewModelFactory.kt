package com.hazlosano.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hazlosano.data.catalog.catalogRepository
import com.hazlosano.domain.usecase.SearchCatalogUseCase

/**
 * Arma la búsqueda con lo que este target tenga.
 *
 * Se recuerda **en la pantalla principal y no dentro de los resultados**, porque el campo vive en la
 * barra de arriba: quien escribe está fuera de la pantalla que su término abre. Construirlo dentro
 * de los resultados sería construirlo después de haberlo necesitado.
 */
@Composable
fun rememberSearchViewModel(): SearchViewModel = remember {
    SearchViewModel(searchCatalog = SearchCatalogUseCase(catalogRepository()))
}
