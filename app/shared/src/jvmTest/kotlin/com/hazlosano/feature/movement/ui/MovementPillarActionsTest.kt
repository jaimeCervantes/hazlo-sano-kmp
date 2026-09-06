package com.hazlosano.feature.movement.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.theme.HazloSanoTheme
import com.hazlosano.domain.model.PillarType
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.presentation.catalogSections
import com.hazlosano.feature.catalog.ui.PillarCatalogContent
import com.hazlosano.feature.pillar.presentation.PillarHighlightsUiState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Las puertas del pilar de Movimiento, y que sólo estén donde deben.
 *
 * El pilar con más código construido de los cuatro era el único cuya herramienta no se alcanzaba
 * desde su propia pestaña: para grabar había que salir a Inicio, y para abrir una ruta había que
 * atravesar el tracker y el historial.
 */
@OptIn(ExperimentalTestApi::class)
class MovementPillarActionsTest {

    private val noHighlights = PillarHighlightsUiState.Loading

    private fun emptyCatalog() = PillarCatalogUiState.Ready(
        sections = catalogSections(emptyList(), nowEpochMillis = 0),
        fromCache = false,
    )

    @Test
    fun `the three doors are there`() = runComposeUiTest {
        setContent {
            HazloSanoTheme {
                MovementPillarActions(
                    onStartOuting = {},
                    onOpenRoutes = {},
                    onOpenOutings = {},
                )
            }
        }

        onNodeWithTag(MovementPillarActionTags.START).assertIsDisplayed()
        onNodeWithTag(MovementPillarActionTags.ROUTES).assertIsDisplayed()
        onNodeWithTag(MovementPillarActionTags.OUTINGS).assertIsDisplayed()
    }

    @Test
    fun `each door reports itself when touched`() {
        val cases = listOf(
            MovementPillarActionTags.START to "start",
            MovementPillarActionTags.ROUTES to "routes",
            MovementPillarActionTags.OUTINGS to "outings",
        )

        for ((tag, expected) in cases) {
            runComposeUiTest {
                val opened = mutableListOf<String>()
                setContent {
                    HazloSanoTheme {
                        MovementPillarActions(
                            onStartOuting = { opened += "start" },
                            onOpenRoutes = { opened += "routes" },
                            onOpenOutings = { opened += "outings" },
                        )
                    }
                }

                onNodeWithTag(tag).performClick()

                assertEquals(listOf(expected), opened)
            }
        }
    }

    /**
     * Empezar una salida se alcanza **desde la pestaña del pilar**, sin pasar por Inicio. El tablero
     * las pinta porque recibe el hueco lleno, no porque sepa qué pilar es.
     */
    @Test
    fun `the movement board offers its tools`() = runComposeUiTest {
        setContent {
            HazloSanoTheme {
                PillarCatalogContent(
                    pillar = PillarType.MOVEMENT,
                    state = emptyCatalog(),
                    highlights = noHighlights,
                    onRetry = {},
                    pillarActions = {
                        MovementPillarActions(
                            onStartOuting = {},
                            onOpenRoutes = {},
                            onOpenOutings = {},
                        )
                    },
                )
            }
        }

        onNodeWithTag(MovementPillarActionTags.START).assertIsDisplayed()
    }

    /** Los otros tres pilares no llenan el hueco, así que su tablero no cambia en nada. */
    @Test
    fun `a pillar with no tools of its own shows none`() = runComposeUiTest {
        setContent {
            HazloSanoTheme {
                PillarCatalogContent(
                    pillar = PillarType.NUTRITION,
                    state = emptyCatalog(),
                    highlights = noHighlights,
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(MovementPillarActionTags.START).assertDoesNotExist()
        onNodeWithTag(MovementPillarActionTags.ROUTES).assertDoesNotExist()
        onNodeWithTag(MovementPillarActionTags.OUTINGS).assertDoesNotExist()
    }
}
