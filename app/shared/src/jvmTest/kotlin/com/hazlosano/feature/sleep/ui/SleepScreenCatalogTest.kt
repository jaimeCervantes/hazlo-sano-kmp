package com.hazlosano.feature.sleep.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.components.sections.PillarHighlightsTags
import com.hazlosano.domain.model.HazloChampion
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.domain.model.PublicationKind
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.presentation.catalogSections
import com.hazlosano.feature.catalog.ui.PillarCatalogTags
import com.hazlosano.feature.pillar.presentation.PillarHighlightsUiState
import kotlin.test.Test

/**
 * El pilar de sueño enseña el catálogo del sitio debajo de su panel de análisis.
 *
 * Lo que esta pantalla tiene de particular es que **ya tenía contenido propio**: el resumen de la
 * última noche es funcionalidad real, no un hueco. Así que lo que se comprueba aquí es que el
 * catálogo se suma sin tapar lo que había, y que un catálogo que no se puede leer no deja la
 * pantalla en blanco.
 */
@OptIn(ExperimentalTestApi::class)
class SleepScreenCatalogTest {

    private val now = 1_788_620_400_000L

    private val highlights = PillarHighlightsUiState.Ready(
        PillarHighlights(
            champions = listOf(
                HazloChampion(
                    name = "Valeria N.",
                    title = "Rutina constante",
                    stat = "7 noches",
                    imageUrl = "",
                ),
            ),
        ),
    )

    private fun product(id: String, name: String) = HazloProduct(
        id = id,
        name = name,
        price = 18.0,
        kind = PublicationKind.PRODUCT,
    )

    private fun ready(publications: List<HazloProduct>, fromCache: Boolean = false) =
        PillarCatalogUiState.Ready(
            sections = catalogSections(publications, now),
            fromCache = fromCache,
        )

    @Test
    fun `the sleep pillar shows the catalogue read from the site`() = runComposeUiTest {
        setContent {
            SleepDashboardContent(
                sleepAnalysis = null,
                highlights = highlights,
                catalogState = ready(listOf(product("p1", "Antifaz de seda"))),
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
            )
        }

        onNodeWithTag(PillarCatalogTags.GRID).assertIsDisplayed()
        onNodeWithText("Antifaz de seda").assertIsDisplayed()
    }

    @Test
    fun `a cached catalogue says so inside the sleep board`() = runComposeUiTest {
        setContent {
            SleepDashboardContent(
                sleepAnalysis = null,
                highlights = highlights,
                catalogState = ready(listOf(product("p1", "Antifaz de seda")), fromCache = true),
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
            )
        }

        onNodeWithTag(PillarCatalogTags.STALE_NOTICE).assertIsDisplayed()
    }

    @Test
    fun `a catalogue that cannot be read does not blank the sleep dashboard`() = runComposeUiTest {
        setContent {
            SleepDashboardContent(
                sleepAnalysis = null,
                highlights = highlights,
                catalogState = PillarCatalogUiState.Unavailable,
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
            )
        }

        // Los campeones de la semana no dependen de la red, y siguen ahí cuando el catálogo falla.
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertIsDisplayed()
        onNodeWithText("Valeria N.").assertIsDisplayed()
        onNodeWithTag(PillarCatalogTags.UNAVAILABLE).assertIsDisplayed()
    }

    @Test
    fun `no second hero card is drawn on top of the sleep summary`() = runComposeUiTest {
        setContent {
            SleepDashboardContent(
                sleepAnalysis = null,
                highlights = highlights,
                catalogState = ready(listOf(product("p1", "Antifaz de seda"))),
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
            )
        }

        // Esta pantalla ya tiene encabezado propio; el resumen del pilar sobraría a media pantalla.
        onNodeWithTag(PillarCatalogTags.SUMMARY).assertDoesNotExist()
    }
}
