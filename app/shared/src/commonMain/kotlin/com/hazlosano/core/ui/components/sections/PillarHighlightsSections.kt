package com.hazlosano.core.ui.components.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.model.PillarHighlights

/** Etiquetas de prueba. El orden del tablero se afirma por aquí y no por su redacción. */
object PillarHighlightsTags {
    const val CHAMPIONS: String = "highlights_champions"
    const val CHALLENGES: String = "highlights_challenges"
}

/**
 * Campeones y retos de un pilar, para insertarlos en cualquier `LazyColumn`.
 *
 * Vive en `core/ui/components/sections/` y no dentro de una pantalla porque lo usan el tablero de
 * pilar y Sueño; dejarlo en una de las dos habría obligado a la otra a importar de una feature
 * ajena.
 *
 * Cada sección se dibuja sólo si tiene algo: un encabezado sobre una fila vacía promete contenido
 * que no llega.
 *
 * El espacio va **antes** de cada sección, como en los carruseles del catálogo. Así el ritmo del
 * tablero no depende de qué secciones haya: una sección que falta no deja un hueco doble.
 */
fun LazyListScope.pillarHighlightsSections(
    highlights: PillarHighlights,
    accent: Color,
) {
    if (highlights.champions.isNotEmpty()) {
        item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
        item {
            HazloChampionsSection(
                champions = highlights.champions,
                modifier = Modifier.testTag(PillarHighlightsTags.CHAMPIONS),
                accentColor = accent,
            )
        }
    }

    if (highlights.challenges.isNotEmpty()) {
        item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
        item {
            HazloChallengesSection(
                challenges = highlights.challenges,
                modifier = Modifier.testTag(PillarHighlightsTags.CHALLENGES),
                accentColor = accent,
            )
        }
    }
}
