package com.hazlosano.feature.catalog.presentation

/**
 * En qué estado está el catálogo de un pilar.
 *
 * Tipo cerrado y **sin una sola cadena redactada**: la palabra la elige la UI leyendo el catálogo
 * de recursos. Un `String` ya escrito aquí sería copia de cara al usuario en una capa que ninguna
 * traducción alcanza, y obligaría a los tests a afirmar sobre la redacción en vez de sobre el
 * estado.
 */
sealed interface PillarCatalogUiState {

    data object Loading : PillarCatalogUiState

    /**
     * Hay algo que enseñar, ya repartido en las secciones del tablero.
     *
     * [fromCache] distingue lo que está publicado ahora de lo que se leyó la última vez que hubo
     * red — la pantalla lo avisa en lugar de hacerlos pasar por lo mismo.
     */
    data class Ready(
        val sections: CatalogSections,
        val fromCache: Boolean = false,
    ) : PillarCatalogUiState

    /** Sin red y sin nada guardado de este pilar: no se inventa un catálogo. */
    data object Unavailable : PillarCatalogUiState

    /** Algo falló de forma inesperada. Distinto de no tener red, y se dice distinto. */
    data object Failed : PillarCatalogUiState
}
