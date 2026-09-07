package com.hazlosano.feature.movement.routes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.feature.movement.routes.presentation.RouteDetailUiState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La pantalla de una ruta guardada, mirada como la mira alguien que la abre desde la lista.
 *
 * El mapa en sí no se dibuja en la JVM —`MovementMap` sólo tiene implementación real en Android—,
 * así que lo que se afirma aquí es **qué decide la pantalla**: si hay trazado que enseñar, qué
 * cifras salen, y qué se dice cuando no hay nada que dibujar.
 */
@OptIn(ExperimentalTestApi::class)
class RouteDetailScreenTest {

    private fun point(lat: Double, lon: Double, altitude: Double? = null) =
        UserLocation(latitude = lat, longitude = lon, altitude = altitude)

    private fun detail(
        name: String = "Cañón del Sumidero",
        distanceMeters: Double = 8_420.0,
        elevationGainMeters: Double? = 350.0,
        pointCount: Int = 412,
        path: List<UserLocation> = listOf(
            point(16.7500, -93.0800, 520.0),
            point(16.7510, -93.0790, 545.0),
        ),
    ) = RouteDetailUiState.Detail(
        name = name,
        distanceMeters = distanceMeters,
        elevationGainMeters = elevationGainMeters,
        pointCount = pointCount,
        path = path,
    )

    @Test
    fun `opening a route draws its track`() = runComposeUiTest {
        setContent { RouteDetailContent(state = detail(), onBack = {}) }

        onNodeWithTag(RouteDetailTags.MAP).assertIsDisplayed()
    }

    @Test
    fun `the route shows its name in the bar`() = runComposeUiTest {
        setContent { RouteDetailContent(state = detail(), onBack = {}) }

        onNodeWithText("Cañón del Sumidero").assertIsDisplayed()
    }

    @Test
    fun `the figures are shown beside the map`() = runComposeUiTest {
        setContent { RouteDetailContent(state = detail(), onBack = {}) }

        onNodeWithTag(RouteDetailTags.METRICS).assertIsDisplayed()
        onNodeWithText("8.42 km").assertIsDisplayed()
        onNodeWithText("350 m").assertIsDisplayed()
        onNodeWithText("412").assertIsDisplayed()
    }

    @Test
    fun `a climb that was never measured is not shown as zero`() = runComposeUiTest {
        // Cero metros de desnivel afirma que la ruta es llana. No saberlo es otra cosa.
        setContent {
            RouteDetailContent(state = detail(elevationGainMeters = null), onBack = {})
        }

        onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun `a route that no longer exists says so instead of showing an empty map`() = runComposeUiTest {
        setContent { RouteDetailContent(state = RouteDetailUiState.Missing, onBack = {}) }

        onNodeWithTag(RouteDetailTags.MISSING).assertIsDisplayed()
        onNodeWithTag(RouteDetailTags.MAP).assertDoesNotExist()
    }

    @Test
    fun `a route saved with no points says it has no track`() = runComposeUiTest {
        setContent {
            RouteDetailContent(state = detail(pointCount = 0, path = emptyList()), onBack = {})
        }

        onNodeWithTag(RouteDetailTags.NO_PATH).assertIsDisplayed()
        onNodeWithTag(RouteDetailTags.MAP).assertDoesNotExist()
    }

    @Test
    fun `a single point is not a track worth drawing`() = runComposeUiTest {
        setContent {
            RouteDetailContent(
                state = detail(pointCount = 1, path = listOf(point(16.75, -93.08))),
                onBack = {},
            )
        }

        onNodeWithTag(RouteDetailTags.NO_PATH).assertIsDisplayed()
    }

    @Test
    fun `the figures are still shown for a route that cannot be drawn`() = runComposeUiTest {
        // Que no haya trazado no borra lo que la ruta sí sabe de sí misma.
        setContent {
            RouteDetailContent(state = detail(pointCount = 0, path = emptyList()), onBack = {})
        }

        onNodeWithTag(RouteDetailTags.METRICS).assertIsDisplayed()
    }

    /**
     * La pantalla pinta su fondo, y no ensena lo que haya detras.
     *
     * Es el fallo que se reporto usando el app: esta era **la unica pantalla del app cuya raiz no
     * pintaba fondo**, asi que al abrir una ruta desde la lista el header salia del color del
     * sistema en vez del del tema. La barra ya se pinta sola desde este slice; esto cubre la otra
     * mitad, el cuerpo, que queda a la vista siempre que el mapa no lo tape — con la ruta cargando o
     * sin ruta que encontrar.
     *
     * El fondo hostil es lo que da valor a la prueba: sobre un padre del color del tema, una raiz
     * transparente y una pintada se ven identicas.
     */
    @Test
    fun `the screen paints its own background instead of showing what is behind`() =
        runComposeUiTest {
            val themeBackground = mutableStateOf(Color.Unspecified)

            setContent {
                MaterialTheme {
                    themeBackground.value = MaterialTheme.colorScheme.background
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFF00FF))) {
                        // Sin ruta que encontrar: es cuando no hay mapa que tape el cuerpo.
                        RouteDetailContent(state = RouteDetailUiState.Missing, onBack = {})
                    }
                }
            }

            // Abajo del todo, no arriba: arriba esta la barra, que desde este slice pinta su
            // propio fondo y taparia el fallo que esta prueba busca. Se comprobo: muestreando la
            // esquina superior, la prueba pasaba con el cuerpo transparente.
            val body = onNodeWithTag(RouteDetailTags.SCREEN).captureToImage().toPixelMap()
            val painted = body[body.width / 2, body.height - 1]

            assertEquals(
                themeBackground.value,
                painted,
                "la pantalla dejo ver lo que tenia detras",
            )
        }
}
