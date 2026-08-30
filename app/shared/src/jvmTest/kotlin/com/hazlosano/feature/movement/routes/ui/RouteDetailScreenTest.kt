package com.hazlosano.feature.movement.routes.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.feature.movement.routes.presentation.RouteDetailUiState
import kotlin.test.Test

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
}
