package com.hazlosano.feature.pillar.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.Color
import com.hazlosano.core.ui.components.sections.pillarHighlightsSections
import com.hazlosano.core.ui.components.sections.pillarHighlightsSkeleton
import com.hazlosano.feature.pillar.presentation.PillarHighlightsUiState

/**
 * Los campeones y retos de un pilar, en cualquiera de los tres estados en que pueden estar.
 *
 * Está aquí y no en `core/ui/components/sections/` porque conoce el estado de presentación; allí
 * viven las dos mitades que sí son puro dibujo —las secciones y su esqueleto—, sin saber de dónde
 * salen. Lo usan el tablero de pilar y Sueño, que es lo que evita repetir este `when` en las dos.
 */
fun LazyListScope.pillarHighlights(
    state: PillarHighlightsUiState,
    accent: Color,
) {
    when (state) {
        PillarHighlightsUiState.Loading -> pillarHighlightsSkeleton()

        is PillarHighlightsUiState.Ready -> pillarHighlightsSections(
            highlights = state.highlights,
            accent = accent,
        )

        // Sin campeones que enseñar no se pinta nada. Un encabezado sobre una fila vacía promete
        // contenido que no llega, y un mensaje de error por unos campeones sería alarmar por algo
        // que no impide usar el pilar.
        PillarHighlightsUiState.Unavailable -> Unit
    }
}
