package com.hazlosano.feature.search.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.presentation.PublicationListUiState
import com.hazlosano.core.ui.components.sections.PillarFilterTags
import com.hazlosano.core.ui.components.sections.PublicationListTags
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La pantalla de resultados de la búsqueda.
 *
 * Spec: `features/busqueda_en_la_barra.feature`, slice 1, y `el_inicio_no_brinca.feature`, slice 2.
 *
 * **Esta clase es deuda saldada.** El slice que mudó la búsqueda a la barra se entregó diciendo que
 * ninguna prueba componía estas pantallas, y era falso: `runComposeUiTest` lleva usándose en este
 * repositorio desde hace slices. La frase venía heredada de bitácoras anteriores y no se comprobó.
 */
@OptIn(ExperimentalTestApi::class)
class SearchResultsScreenTest {

    private val fromNutrition = HazloProduct(
        id = "miel-1",
        name = "Miel de abeja melipona",
        description = "De un meliponario familiar en Yucatán.",
        price = 180.0,
        imageUrl = "",
        category = "alimentacion",
        pillar = PillarType.NUTRITION,
    )

    @Test
    fun `the results carry their own pillar chips`() = runComposeUiTest {
        setContent {
            SearchResultsScreen(
                results = PublicationListUiState(
                    publications = listOf(fromNutrition),
                    loading = false,
                    firstPagePending = false,
                ),
                onSelectFilter = {},
                onReachedEnd = {},
                onRetry = {},
            )
        }

        onNodeWithTag(SearchTags.RESULTS).assertIsDisplayed()
        onNodeWithTag(PillarFilterTags.ROW).assertIsDisplayed()
        onNodeWithTag(PublicationListTags.publication("miel-1")).assertIsDisplayed()
    }

    /** Tocar un chip acota los resultados, y quien decide qué significa eso es el ViewModel. */
    @Test
    fun `choosing a pillar chip narrows the results`() = runComposeUiTest {
        var chosen: PillarType? = null
        var called = false

        setContent {
            SearchResultsScreen(
                results = PublicationListUiState(
                    publications = listOf(fromNutrition),
                    loading = false,
                    firstPagePending = false,
                ),
                onSelectFilter = { chosen = it; called = true },
                onReachedEnd = {},
                onRetry = {},
            )
        }

        onNodeWithTag(PillarFilterTags.chip(PillarType.NUTRITION)).performClick()

        assertEquals(true, called, "el chip no avisó a nadie")
        assertEquals(PillarType.NUTRITION, chosen)
    }

    /**
     * Un término que el sitio no encontró se dice sobre el catálogo, no sobre lo cargado.
     *
     * Es la redacción que el slice 2 de publicaciones ganó al dejar de filtrar en el dispositivo, y
     * hasta ahora no la miraba nadie.
     */
    @Test
    fun `a term the site found nothing for says so`() = runComposeUiTest {
        setContent {
            SearchResultsScreen(
                results = PublicationListUiState(loading = false, firstPagePending = false),
                onSelectFilter = {},
                onReachedEnd = {},
                onRetry = {},
            )
        }

        onNodeWithTag(PublicationListTags.EMPTY).assertIsDisplayed()
    }

    /** Y mientras el sitio contesta, el mismo hueco que el feed. */
    @Test
    fun `while the site answers it shows the same gap as the feed`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            SearchResultsScreen(
                results = PublicationListUiState(loading = true),
                onSelectFilter = {},
                onReachedEnd = {},
                onRetry = {},
            )
        }

        onNodeWithTag(PublicationListTags.SKELETON).assertIsDisplayed()
        onNodeWithTag(PublicationListTags.LOADING_MORE).assertDoesNotExist()
    }
}
