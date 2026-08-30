package com.hazlosano.feature.catalog.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.PublicationKind
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.presentation.catalogSections
import com.hazlosano.feature.pillar.presentation.PillarHighlightsUiState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Cómo se llega desde un pilar a qué es ese pilar.
 *
 * La puerta está en la tarjeta de resumen, junto a actualizar: es la pregunta que se hace quien abre
 * la pestaña por primera vez, y esconderla en el título la habría dejado sin encontrar.
 */
@OptIn(ExperimentalTestApi::class)
class PillarBoardInfoTest {

    private val now = 1_788_620_400_000L

    private fun ready() = PillarCatalogUiState.Ready(
        sections = catalogSections(
            listOf(
                HazloProduct(
                    id = "p1",
                    name = "Zapatillas de trail",
                    price = 120.0,
                    kind = PublicationKind.PRODUCT,
                ),
            ),
            now,
        ),
        fromCache = false,
    )

    @Test
    fun `the summary card opens what this pillar is`() = runComposeUiTest {
        var opened = false

        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = ready(),
                highlights = PillarHighlightsUiState.Ready(PillarHighlights()),
                onRetry = {},
                onOpenInfo = { opened = true },
            )
        }

        onNodeWithTag(PillarSummaryCardTags.INFO).performClick()

        assertTrue(opened, "la tarjeta del pilar abre su explicación")
    }

    @Test
    fun `a board with no catalogue yet offers no summary card to open it from`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = PillarCatalogUiState.Unavailable,
                highlights = PillarHighlightsUiState.Ready(PillarHighlights()),
                onRetry = {},
                onOpenInfo = {},
            )
        }

        // Sin catálogo no hay tarjeta de resumen, así que tampoco su botón. Queda pendiente darle
        // otra puerta a la explicación del pilar cuando no hay red.
        onNodeWithTag(PillarSummaryCardTags.INFO).assertDoesNotExist()
    }
}
