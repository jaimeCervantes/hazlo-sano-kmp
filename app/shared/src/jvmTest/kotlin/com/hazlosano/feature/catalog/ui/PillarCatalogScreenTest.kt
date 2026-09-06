package com.hazlosano.feature.catalog.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.PublicationKind
import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.presentation.catalogSections
import com.hazlosano.feature.pillar.presentation.PillarHighlightsUiState
import kotlin.test.Test

/**
 * El tablero del pilar, probado como lo ve alguien que abre la pestaña.
 *
 * Es el bucle exterior del slice: mira la pantalla compuesta, no el estado. Las afirmaciones van
 * sobre etiquetas de prueba para la estructura y sobre el texto que **sale de los datos** para el
 * contenido — no sobre la redacción del catálogo de cadenas, que puede cambiar sin que el tablero
 * esté roto.
 */
@OptIn(ExperimentalTestApi::class)
class PillarCatalogScreenTest {

    private val now = 1_788_620_400_000L // 2026-09-05T15:00:00Z

    // Lo que este test mira es el catálogo; los campeones y retos del pilar tienen los suyos en
    // `PillarBoardOrderTest`, así que aquí el tablero se compone sin ellos.
    private val noHighlights = PillarHighlightsUiState.Ready(PillarHighlights())

    private fun event(id: String, name: String, startsAt: Long?, endsAt: Long? = null) = HazloProduct(
        id = id,
        name = name,
        kind = PublicationKind.EVENT,
        startsAtEpochMillis = startsAt,
        endsAtEpochMillis = endsAt,
    )

    private fun product(id: String, name: String, price: Double? = 45.0, distance: Double? = null) =
        HazloProduct(
            id = id,
            name = name,
            price = price,
            kind = PublicationKind.PRODUCT,
            distanceMeters = distance,
        )

    private fun service(id: String, name: String, minutes: Int) = HazloProduct(
        id = id,
        name = name,
        price = 450.0,
        kind = PublicationKind.SERVICE,
        durationMinutes = minutes,
    )

    private fun ready(publications: List<HazloProduct>, fromCache: Boolean = false) =
        PillarCatalogUiState.Ready(
            sections = catalogSections(publications, now),
            fromCache = fromCache,
        )

    @Test
    fun `the pillar opens with a summary of what it holds`() = runComposeUiTest {
        val publications = buildList {
            repeat(7) { add(product("p$it", "Producto $it")) }
            add(event("e1", "Rodada del domingo", now + 86_400_000L))
            add(event("e2", "Taller de respiración", now + 172_800_000L))
            add(event("e3", "Caminata nocturna", now + 259_200_000L))
            add(service("s1", "Masaje deportivo", 60))
            add(service("s2", "Consulta nutricional", 45))
        }

        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = ready(publications),
                highlights = noHighlights,
                onRetry = {},
            )
        }

        onNodeWithTag(PillarCatalogTags.SUMMARY).assertIsDisplayed()
        onNodeWithText("12").assertIsDisplayed()
        onNodeWithText("3").assertIsDisplayed()
        onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun `a pillar with no events does not draw the events section`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.NUTRITION,
                state = ready(listOf(product("p1", "Suero natural"))),
                highlights = noHighlights,
                onRetry = {},
            )
        }

        onNodeWithTag(PillarCatalogTags.EVENTS).assertDoesNotExist()
    }

    /**
     * «Servicios» y «Cerca de ti» se quitaron: la búsqueda que cierra el tablero ya enseña lo mismo
     * unas pulgadas más abajo, y «Cerca de ti» además reordenaba por distancia algo que el sitio ya
     * devuelve ordenado por distancia. Se afirma que **no vuelven**, con un pilar que tiene de las
     * dos cosas: un servicio y una publicación con distancia.
     */
    @Test
    fun `the board no longer repeats what the search below already shows`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.NUTRITION,
                state = ready(
                    listOf(
                        service("s1", "Consulta de nutrición", minutes = 45),
                        product("p1", "Suero natural", distance = 340.0),
                    ),
                ),
                highlights = noHighlights,
                onRetry = {},
            )
        }

        onNodeWithTag("catalog_services").assertDoesNotExist()
        onNodeWithTag("catalog_nearby").assertDoesNotExist()
        // Lo que sí sigue: la búsqueda, que es lo que las cubre.
        onNodeWithTag(PillarCatalogTags.GRID).assertExists()
    }

    @Test
    fun `an upcoming event gets its own section`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MOVEMENT,
                state = ready(listOf(event("e1", "Rodada del domingo", now + 86_400_000L))),
                highlights = noHighlights,
                onRetry = {},
            )
        }

        onNodeWithTag(PillarCatalogTags.EVENTS).assertIsDisplayed()
    }

    @Test
    fun `the stale notice sits above the summary`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.SLEEP,
                state = ready(listOf(product("p1", "Antifaz")), fromCache = true),
                highlights = noHighlights,
                onRetry = {},
            )
        }

        onNodeWithTag(PillarCatalogTags.STALE_NOTICE).assertIsDisplayed()
        onNodeWithTag(PillarCatalogTags.SUMMARY).assertIsDisplayed()
    }

    @Test
    fun `fresh content carries no stale notice`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.SLEEP,
                state = ready(listOf(product("p1", "Antifaz"))),
                highlights = noHighlights,
                onRetry = {},
            )
        }

        onNodeWithTag(PillarCatalogTags.STALE_NOTICE).assertDoesNotExist()
    }

    @Test
    fun `a pillar never downloaded draws no board at all`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MIND,
                state = PillarCatalogUiState.Unavailable,
                highlights = noHighlights,
                onRetry = {},
            )
        }

        onNodeWithTag(PillarCatalogTags.UNAVAILABLE).assertIsDisplayed()
        onNodeWithTag(PillarCatalogTags.SUMMARY).assertDoesNotExist()
        onNodeWithTag(PillarCatalogTags.GRID).assertDoesNotExist()
    }

    @Test
    fun `a service shows how long it lasts`() = runComposeUiTest {
        setContent {
            PillarCatalogContent(
                pillar = PillarType.MIND,
                state = ready(listOf(service("s1", "Masaje deportivo", 60))),
                highlights = noHighlights,
                onRetry = {},
            )
        }

        // Desde que se quitó el carrusel de servicios, la rejilla de búsqueda es el único sitio donde
        // esta línea se ve — y sigue viéndose, que es lo que este test cuida.
        onNodeWithText("60 min").assertIsDisplayed()
    }
}
