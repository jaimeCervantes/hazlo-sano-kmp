package com.hazlosano.feature.movement.routes.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.components.AppBarMenuTags
import com.hazlosano.core.ui.components.atomic.HazloTopAppBarTags
import com.hazlosano.feature.movement.ui.MovementBottomBarTags
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

    // ─────────── La acción principal: salir a seguir esta ruta ───────────

    /**
     * A mirar una ruta se viene para decidir si se hace, así que la acción de hacerla está delante y
     * no escondida en el menú. Antes había que volver a la lista, entrar al tracker y buscar la ruta
     * en un diálogo, teniéndola ya en la pantalla.
     */
    @Test
    fun `a route can be started from where it is being looked at`() = runComposeUiTest {
        var started = 0
        setContent {
            RouteDetailContent(state = detail(), onBack = {}, onStartOuting = { started++ })
        }

        onNodeWithTag(RouteDetailTags.START).performClick()

        assertEquals(1, started)
    }

    /** Sin ruta delante no hay nada que empezar, y el botón no se ofrece. */
    @Test
    fun `a missing route offers nothing to start`() = runComposeUiTest {
        setContent { RouteDetailContent(state = RouteDetailUiState.Missing, onBack = {}) }

        onNodeWithTag(RouteDetailTags.START).assertDoesNotExist()
    }

    /**
     * Un detalle no es un sitio del pilar, así que no lleva la barra de sitios.
     *
     * Es la otra mitad de la regla del slice: los tres lugares la llevan y los dos detalles no. Un
     * detalle es algo que se abrió **desde** un sitio y de lo que se sale volviendo atrás; darle
     * destinos hermanos invita a perderse en vez de a volver.
     */
    @Test
    fun `a detail is not a place and carries no place bar`() = runComposeUiTest {
        setContent { RouteDetailContent(state = detail(), onBack = {}) }

        onNodeWithTag(MovementBottomBarTags.BAR).assertDoesNotExist()
    }
}
