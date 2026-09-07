package com.hazlosano.feature.movement.routes.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.components.AppBarMenuTags
import com.hazlosano.core.ui.components.atomic.HazloTopAppBarTags
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.feature.movement.routes.presentation.RouteDetailUiState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El menú de los tres puntos del detalle de una ruta.
 *
 * Junta las dos clases de opción que puede haber en una barra: **lo que se le hace a lo que estás
 * mirando** —descargar y borrar esta ruta— y **lo del app**, que es Ajustes y está siempre porque no
 * depende de que la ruta exista.
 *
 * Ajustes vive aquí por un motivo concreto: antes sólo se llegaba por el ⋮ de la pantalla principal,
 * así que quien estaba dentro del pilar tenía que salirse entero para cambiar el tema o el idioma.
 */
@OptIn(ExperimentalTestApi::class)
class RouteDetailMenuTest {

    private fun detail() = RouteDetailUiState.Detail(
        name = "Cañón del Sumidero",
        distanceMeters = 8_420.0,
        elevationGainMeters = 350.0,
        pointCount = 412,
        path = listOf(
            UserLocation(latitude = 16.7500, longitude = -93.0800),
            UserLocation(latitude = 16.7510, longitude = -93.0790),
        ),
    )

    @Test
    fun `the three dots open the route's own actions`() = runComposeUiTest {
        setContent { RouteDetailContent(state = detail(), onBack = {}) }

        onNodeWithTag(HazloTopAppBarTags.MENU).performClick()

        onNodeWithTag(RouteDetailTags.DELETE).assertIsDisplayed()
    }

    @Test
    fun `settings is reachable without leaving the route`() = runComposeUiTest {
        var openedSettings = 0
        setContent {
            RouteDetailContent(
                state = detail(),
                onBack = {},
                onOpenSettings = { openedSettings++ },
            )
        }

        onNodeWithTag(HazloTopAppBarTags.MENU).performClick()
        onNodeWithTag(AppBarMenuTags.SETTINGS).performClick()

        assertEquals(1, openedSettings)
    }

    /**
     * Y sigue estando cuando no hay ruta que enseñar.
     *
     * Es la diferencia entre las dos clases de opción: descargar y borrar necesitan una ruta;
     * Ajustes no, así que un detalle que no encontró nada tampoco puede dejarte sin salida.
     */
    @Test
    fun `settings is there even when the route is missing`() = runComposeUiTest {
        var openedSettings = 0
        setContent {
            RouteDetailContent(
                state = RouteDetailUiState.Missing,
                onBack = {},
                onOpenSettings = { openedSettings++ },
            )
        }

        onNodeWithTag(HazloTopAppBarTags.MENU).performClick()
        onNodeWithTag(AppBarMenuTags.SETTINGS).performClick()

        assertEquals(1, openedSettings)
    }

    /** Sin ruta no hay nada que borrar, y la opción no se ofrece. */
    @Test
    fun `a missing route offers nothing to delete`() = runComposeUiTest {
        setContent { RouteDetailContent(state = RouteDetailUiState.Missing, onBack = {}) }

        onNodeWithTag(HazloTopAppBarTags.MENU).performClick()

        onNodeWithTag(RouteDetailTags.DELETE).assertDoesNotExist()
    }
}
