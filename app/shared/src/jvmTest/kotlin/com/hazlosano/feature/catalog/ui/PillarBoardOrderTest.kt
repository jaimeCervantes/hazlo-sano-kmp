package com.hazlosano.feature.catalog.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.components.sections.PillarHighlightsTags
import com.hazlosano.domain.model.HazloChallenge
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
 * El orden del tablero, probado como lo ve alguien que abre la pestaña.
 *
 * Es el bucle exterior del slice: el orden del proyecto Android de referencia —identidad del pilar,
 * campeones, retos y, al final, buscar— comprobado por la posición real de cada sección en pantalla.
 *
 * Cada prueba compone sólo las secciones que compara. Un `LazyColumn` no compone lo que queda fuera
 * de la ventana, así que un tablero con todo dentro dejaría la última sección sin nodo que medir.
 */
@OptIn(ExperimentalTestApi::class)
class PillarBoardOrderTest {

    private val now = 1_788_620_400_000L // 2026-09-05T15:00:00Z

    private fun product(id: String, name: String) = HazloProduct(
        id = id,
        name = name,
        price = 120.0,
        kind = PublicationKind.PRODUCT,
    )

    private fun event(id: String, name: String) = HazloProduct(
        id = id,
        name = name,
        kind = PublicationKind.EVENT,
        startsAtEpochMillis = now + 86_400_000L,
    )

    private fun ready(publications: List<HazloProduct>) = PillarCatalogUiState.Ready(
        sections = catalogSections(publications, now),
        fromCache = false,
    )

    private val champion = HazloChampion(
        name = "Mateo R.",
        title = "Top Trekker",
        stat = "42.5 km",
        imageUrl = "",
    )

    private val challenge = HazloChallenge(
        title = "Montañero 50k",
        description = "Acumula 50 km de elevación en los próximos 30 días.",
        progressText = "12 km / 50 km",
        progress = 0.24f,
        imageUrl = "",
    )

    private fun highlights(
        champions: List<HazloChampion> = listOf(champion),
        challenges: List<HazloChallenge> = listOf(challenge),
    ) = PillarHighlightsUiState.Ready(PillarHighlights(champions, challenges))

    /** Dónde empieza una sección en la pantalla. Es lo que hace comprobable "va antes que". */
    private fun ComposeUiTest.topOf(tag: String): Float =
        onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    @Test
    fun `the pillar leads, then the champions, then the challenges`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = ready(listOf(product("p1", "Zapatillas de trail"))),
                highlights = highlights(),
                onRetry = {},
            )
        }

        val summary = topOf(PillarCatalogTags.SUMMARY)
        val champions = topOf(PillarHighlightsTags.CHAMPIONS)
        val challenges = topOf(PillarHighlightsTags.CHALLENGES)

        assertTrue(summary < champions, "el resumen del pilar va primero")
        assertTrue(champions < challenges, "los campeones van antes que los retos")
    }

    @Test
    fun `the catalogue carousels come after the community`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = ready(listOf(event("e1", "Rodada del domingo"))),
                highlights = highlights(challenges = emptyList()),
                onRetry = {},
            )
        }

        assertTrue(
            topOf(PillarHighlightsTags.CHAMPIONS) < topOf(PillarCatalogTags.EVENTS),
            "los campeones van antes que los próximos eventos",
        )
    }

    @Test
    fun `the search closes the board`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = ready(listOf(product("p1", "Zapatillas de trail"))),
                highlights = highlights(champions = emptyList()),
                onRetry = {},
            )
        }

        assertTrue(
            topOf(PillarHighlightsTags.CHALLENGES) < topOf(PillarCatalogTags.GRID),
            "buscar productos y servicios cierra el tablero",
        )
    }

    @Test
    fun `a pillar with no champions draws no champions section`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.NUTRITION,
                state = ready(listOf(product("p1", "Suero natural"))),
                highlights = highlights(champions = emptyList()),
                onRetry = {},
            )
        }

        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertDoesNotExist()
        onNodeWithTag(PillarHighlightsTags.CHALLENGES).assertIsDisplayed()
    }

    @Test
    fun `a pillar with no open challenges draws no challenges section`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.NUTRITION,
                state = ready(listOf(product("p1", "Suero natural"))),
                highlights = highlights(challenges = emptyList()),
                onRetry = {},
            )
        }

        onNodeWithTag(PillarHighlightsTags.CHALLENGES).assertDoesNotExist()
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertIsDisplayed()
    }

    @Test
    fun `highlights that have not arrived do not hold up the board`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MIND,
                state = ready(listOf(product("p1", "Diario de reflexión"))),
                highlights = PillarHighlightsUiState.Loading,
                onRetry = {},
            )
        }

        onNodeWithTag(PillarCatalogTags.SUMMARY).assertIsDisplayed()
        onNodeWithTag(PillarCatalogTags.GRID).assertIsDisplayed()
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertDoesNotExist()
    }
}
