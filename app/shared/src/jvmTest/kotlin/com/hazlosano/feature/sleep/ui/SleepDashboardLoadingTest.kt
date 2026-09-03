package com.hazlosano.feature.sleep.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.components.sections.PillarHighlightsTags
import com.hazlosano.domain.model.HazloChampion
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.domain.model.PublicationKind
import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.presentation.catalogSections
import com.hazlosano.feature.catalog.ui.PillarCatalogTags
import com.hazlosano.feature.pillar.presentation.PillarHighlightsUiState
import com.hazlosano.feature.sleep.presentation.SleepUiState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Las tres esperas de la pestaña de sueño son independientes: el análisis de anoche, los campeones y
 * el catálogo. Lo que se comprueba aquí es que ninguna arrastra a las otras.
 *
 * Los huecos laten, así que cada prueba detiene antes el reloj con `mainClock.autoAdvance = false`.
 */
@OptIn(ExperimentalTestApi::class)
class SleepDashboardLoadingTest {

    private val now = 1_788_620_400_000L

    private val analysis = SleepAnalysis(
        totalSessions = 2,
        totalDurationMillis = 7 * 60 * 60 * 1000L,
        efficiency = 0.91f,
        periodStart = 1_788_534_000_000L,
        periodEnd = 1_788_570_000_000L,
    )

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

    private fun readyCatalog() = PillarCatalogUiState.Ready(
        sections = catalogSections(
            listOf(
                HazloProduct(
                    id = "p1",
                    name = "Antifaz de seda",
                    price = 18.0,
                    kind = PublicationKind.PRODUCT,
                ),
            ),
            now,
        ),
        fromCache = false,
    )

    @Test
    fun `an analysis still being worked out does not blank the sleep tab`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            SleepDashboardContent(
                state = SleepUiState.Loading,
                highlights = highlights,
                catalogState = readyCatalog(),
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
            )
        }

        onNodeWithTag(SleepTags.SUMMARY_GAP).assertIsDisplayed()
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertIsDisplayed()
        onNodeWithTag(PillarCatalogTags.GRID).assertIsDisplayed()
    }

    @Test
    fun `a night that could not be read says so and leaves the rest standing`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            SleepDashboardContent(
                state = SleepUiState.Failed,
                highlights = highlights,
                catalogState = readyCatalog(),
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
            )
        }

        onNodeWithTag(SleepTags.FAILED).assertIsDisplayed()
        onNodeWithTag(SleepTags.SUMMARY_GAP).assertDoesNotExist()
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertIsDisplayed()
    }

    @Test
    fun `a night that could not be read offers a retry that asks again`() = runComposeUiTest {
        mainClock.autoAdvance = false
        var retried = false

        setContent {
            SleepDashboardContent(
                state = SleepUiState.Failed,
                highlights = highlights,
                catalogState = readyCatalog(),
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
                onRetry = { retried = true },
            )
        }

        onNodeWithTag(SleepTags.RETRY).performClick()

        assertTrue(retried, "el botón de reintentar de Sueño vuelve a pedir el análisis")
    }

    @Test
    fun `a night with no data leaves no gap waiting for it`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            SleepDashboardContent(
                state = SleepUiState.Success(sleepAnalysis = null),
                highlights = highlights,
                catalogState = readyCatalog(),
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
            )
        }

        // No es que esté cargando: es que todavía no hay noche que contar. Un hueco eterno haría
        // creer lo contrario.
        onNodeWithTag(SleepTags.SUMMARY_GAP).assertDoesNotExist()
        onNodeWithTag(PillarHighlightsTags.CHAMPIONS).assertIsDisplayed()
    }

    @Test
    fun `the summary shows up as soon as the night is worked out`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            SleepDashboardContent(
                state = SleepUiState.Success(sleepAnalysis = analysis),
                highlights = highlights,
                catalogState = PillarCatalogUiState.Loading,
                onRetryCatalog = {},
                onRefresh = null,
                onCardClick = {},
            )
        }

        onNodeWithText("Última noche").assertIsDisplayed()
        onNodeWithTag(SleepTags.SUMMARY_GAP).assertDoesNotExist()
    }
}
