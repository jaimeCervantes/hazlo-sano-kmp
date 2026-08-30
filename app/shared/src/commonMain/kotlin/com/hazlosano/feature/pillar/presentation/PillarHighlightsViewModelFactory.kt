package com.hazlosano.feature.pillar.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hazlosano.data.repository.SamplePillarHighlightsRepository
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.usecase.GetPillarHighlightsUseCase

/**
 * Arma los destacados de un pilar. No hay contenedor de inyección en el proyecto, así que la
 * composición pasa aquí, como en el catálogo y en las rutas.
 *
 * La clave del `remember` es el pilar: cambiar de pestaña no debe reutilizar los campeones de la
 * anterior.
 */
@Composable
fun rememberPillarHighlightsViewModel(pillar: PillarType): PillarHighlightsViewModel =
    remember(pillar) {
        PillarHighlightsViewModel(
            pillar = pillar,
            getPillarHighlights = GetPillarHighlightsUseCase(SamplePillarHighlightsRepository()),
        )
    }
