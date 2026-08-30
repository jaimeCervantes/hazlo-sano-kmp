package com.hazlosano.feature.catalog.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.components.sections.PillarBoardSkeletonTags
import com.hazlosano.core.ui.components.sections.PillarHighlightsTags
import com.hazlosano.domain.model.HazloChampion
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
 * Qué se ve mientras el catálogo todavía no ha llegado.
 *
 * El esqueleto late, y una animación infinita nunca deja quieto al reloj de prueba; por eso cada
 * prueba lo detiene con `mainClock.autoAdvance = false` antes de componer. Sin eso, la espera de
 * inactividad de Compose no terminaría nunca.
 */
@OptIn(ExperimentalTestApi::class)
class PillarBoardSkeletonTest {

    private val now = 1_788_620_400_000L

    private val oneChampion = PillarHighlightsUiState.Ready(
        PillarHighlights(
            champions = listOf(
                HazloChampion(
                    name = "Mateo R.",
                    title = "Top Trekker",
                    stat = "42.5 km",
                    imageUrl = "",
                ),
            ),
        ),
    )

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
    fun `a catalogue that has not arrived does not blank the board`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = PillarCatalogUiState.Loading,
                highlights = oneChampion,
                onRetry = {},
            )
        }

        // Lo que no depende de la red se ve ya; sólo lo que falta enseña su hueco.
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertIsDisplayed()
        onNodeWithTag(PillarBoardSkeletonTags.SUMMARY).assertIsDisplayed()
        onNodeWithTag(PillarBoardSkeletonTags.GRID).assertIsDisplayed()
    }

    @Test
    fun `the grid gap keeps the two columns of the real grid`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = PillarCatalogUiState.Loading,
                highlights = PillarHighlightsUiState.Unavailable,
                onRetry = {},
            )
        }

        val cards = onAllNodesWithTag(PillarBoardSkeletonTags.GRID_CARD).fetchSemanticsNodes()

        assertTrue(cards.size >= 2, "el hueco de la rejilla pinta al menos una fila completa")
        assertTrue(
            cards[0].positionInRoot.y == cards[1].positionInRoot.y,
            "las dos primeras tarjetas comparten fila",
        )
        assertTrue(
            cards[0].size.width == cards[1].size.width,
            "las dos columnas miden lo mismo",
        )
    }

    @Test
    fun `a board that is ready shows no gaps`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = ready(),
                highlights = oneChampion,
                onRetry = {},
            )
        }

        onNodeWithTag(PillarBoardSkeletonTags.SUMMARY).assertDoesNotExist()
        onNodeWithTag(PillarBoardSkeletonTags.CAROUSEL).assertDoesNotExist()
        onNodeWithTag(PillarBoardSkeletonTags.CHAMPIONS).assertDoesNotExist()
    }

    @Test
    fun `champions that have not arrived show their own gap`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = ready(),
                highlights = PillarHighlightsUiState.Loading,
                onRetry = {},
            )
        }

        onNodeWithTag(PillarBoardSkeletonTags.CHAMPIONS).assertIsDisplayed()
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertDoesNotExist()
    }

    @Test
    fun `a catalogue that cannot be read says so instead of waiting forever`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MIND,
                state = PillarCatalogUiState.Unavailable,
                highlights = oneChampion,
                onRetry = {},
            )
        }

        // Ni tablero fantasma ni hueco eterno: se dice que hace falta conexión, y los campeones
        // —que no dependen de ella— siguen ahí.
        onNodeWithTag(PillarCatalogTags.UNAVAILABLE).assertIsDisplayed()
        onNodeWithTag(PillarBoardSkeletonTags.SUMMARY).assertDoesNotExist()
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertIsDisplayed()
    }
}
