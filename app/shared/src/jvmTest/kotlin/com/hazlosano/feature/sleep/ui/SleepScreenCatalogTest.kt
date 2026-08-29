package com.hazlosano.feature.sleep.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PublicationKind
import com.hazlosano.domain.model.SleepContent
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.presentation.catalogSections
import com.hazlosano.feature.catalog.ui.PillarCatalogTags
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

    private val content = SleepContent(
        heroTitle = "Tu descanso",
        heroSubtitle = "Esta semana",
        heroMetricLabel = "Promedio",
        heroMetricValue = "7h 10m",
        heroMetricSupport = "de 8h",
        heroProgress = 0.9f,
        heroImageUrl = "",
        weeklyChampions = emptyList(),
        activeChallenges = emptyList(),
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
                content = content,
                sleepAnalysis = null,
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
                content = content,
                sleepAnalysis = null,
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
                content = content,
                sleepAnalysis = null,
                catalogState = PillarCatalogUiState.Unavailable,
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
            )
        }

        // El análisis del sueño es lo que esta pestaña siempre supo hacer, y no depende de la red.
        onNodeWithText("Campeones Semanales").assertIsDisplayed()
        onNodeWithTag(PillarCatalogTags.UNAVAILABLE).assertIsDisplayed()
    }

    @Test
    fun `no second hero card is drawn on top of the sleep summary`() = runComposeUiTest {
        setContent {
            SleepDashboardContent(
                content = content,
                sleepAnalysis = null,
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
