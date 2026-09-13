package com.hazlosano.feature.movement.presentation

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * Que la interfaz se entere de que se ha navegado.
 *
 * **Esta clase existe por un fallo que llegó al teléfono.** `lastPlace` y `lastVisitedPlace` eran
 * `var` corrientes, no estado de Compose, y funcionaban de carambola: la pantalla del pilar leía
 * además `depth` —que sale de `backStack`, que sí es observable— así que cualquier navegación la
 * recomponía y de paso se releía todo lo demás.
 *
 * Al sacar `depth` del estado de la animación, esa lectura desapareció y con ella la única
 * suscripción. El resultado en el teléfono: se abrían los tres sitios, pero tocar una ruta no abría
 * nada. Lo que seguía funcionando era lo que cambia estado local —el diálogo de renombrar— y lo que
 * cerraba el pilar, porque eso sí toca `backStack`.
 *
 * Ninguna prueba de las que había podía verlo: todas llaman a `MovementNavState` y leen sus
 * propiedades desde Kotlin normal, donde un `var` se lee igual de bien. Hace falta **una composición
 * de verdad** que lea la propiedad y tenga que recomponerse sola.
 */
@OptIn(ExperimentalTestApi::class)
class MovementNavStateObservabilityTest {

    private companion object {
        const val WHERE = "donde"
    }

    @Test
    fun `the screen sees the pillar move`() = runComposeUiTest {
        val nav = MovementNavState()
        nav.openTracker()

        setContent {
            Text(text = nav.lastPlace.describe(), modifier = Modifier.testTag(WHERE))
        }

        onNodeWithTag(WHERE).assertTextEquals("Tracker")

        // Lo que en el teléfono era tocar una ruta de la lista.
        nav.openRouteDetail(routeId = 3)

        onNodeWithTag(WHERE).assertTextEquals("RouteDetail")
    }

    /** Y de lo mismo depende el cromo: sin esto, la barra de abajo se queda marcando el sitio viejo. */
    @Test
    fun `the chrome sees which place it is in`() = runComposeUiTest {
        val nav = MovementNavState()
        nav.openTracker()

        setContent {
            Text(text = nav.lastVisitedPlace.describe(), modifier = Modifier.testTag(WHERE))
        }

        onNodeWithTag(WHERE).assertTextEquals("Tracker")

        nav.openRoutes()

        onNodeWithTag(WHERE).assertTextEquals("Routes")
    }

    /** Y volver también se ve. */
    @Test
    fun `going back is seen too`() = runComposeUiTest {
        val nav = MovementNavState()
        nav.openRoutes()
        nav.openRouteDetail(routeId = 3)

        setContent {
            Text(text = nav.lastPlace.describe(), modifier = Modifier.testTag(WHERE))
        }

        onNodeWithTag(WHERE).assertTextEquals("RouteDetail")

        nav.back()

        onNodeWithTag(WHERE).assertTextEquals("Routes")
    }
}

/** El nombre del destino sin lo que lleva dentro, que es lo único que hace falta afirmar aquí. */
private fun MovementDestination.describe(): String = when (this) {
    is MovementDestination.Tracker -> "Tracker"
    MovementDestination.Routes -> "Routes"
    MovementDestination.History -> "History"
    is MovementDestination.RouteDetail -> "RouteDetail"
    is MovementDestination.SessionDetail -> "SessionDetail"
    MovementDestination.Closed -> "Closed"
}
