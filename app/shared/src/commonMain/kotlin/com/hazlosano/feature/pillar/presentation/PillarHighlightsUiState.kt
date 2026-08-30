package com.hazlosano.feature.pillar.presentation

import com.hazlosano.domain.model.PillarHighlights

/**
 * En qué estado están los destacados de un pilar.
 *
 * Sin una sola cadena redactada, como el estado del catálogo: la palabra la elige la UI leyendo el
 * catálogo de recursos.
 */
sealed interface PillarHighlightsUiState {

    data object Loading : PillarHighlightsUiState

    data class Ready(val highlights: PillarHighlights) : PillarHighlightsUiState

    /**
     * No se pudieron leer. La pantalla no pinta las secciones en vez de enseñarlas vacías: un
     * encabezado sobre una fila sin nada promete contenido que no llega.
     */
    data object Unavailable : PillarHighlightsUiState
}
