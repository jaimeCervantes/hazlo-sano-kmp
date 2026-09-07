package com.hazlosano.feature.movement.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La barra que dice por dónde se mueve uno dentro del pilar.
 *
 * Existe por una queja de uso: al entrar al tracker no se alcanzaban «Mis rutas» ni «Mis salidas», y
 * en «Mis salidas» la puerta a «Mis rutas» estaba metida dentro del contenido. La regla que impone
 * es **abajo se va a sitios, en el ⋮ se hacen cosas**.
 *
 * Spec: `features/pulido_de_movimiento.feature`, slice 5.
 */
@OptIn(ExperimentalTestApi::class)
class MovementBottomBarTest {

    @Test
    fun `the three places of the pillar are always all three`() = runComposeUiTest {
        setContent {
            MovementBottomBar(
                current = MovementPlace.Routes,
                onGoToRecord = {},
                onGoToRoutes = {},
                onGoToOutings = {},
            )
        }

        MovementPlace.entries.forEach { place ->
            onNodeWithTag(MovementBottomBarTags.place(place)).assertIsDisplayed()
        }
    }

    @Test
    fun `going somewhere else says where`() = runComposeUiTest {
        var wentTo: MovementPlace? = null
        setContent {
            MovementBottomBar(
                current = MovementPlace.Record,
                onGoToRecord = { wentTo = MovementPlace.Record },
                onGoToRoutes = { wentTo = MovementPlace.Routes },
                onGoToOutings = { wentTo = MovementPlace.Outings },
            )
        }

        onNodeWithTag(MovementBottomBarTags.place(MovementPlace.Routes)).performClick()

        assertEquals(MovementPlace.Routes, wentTo)
    }

    /**
     * Pulsar donde ya estás no viaja.
     *
     * La pila corta hasta el destino en vez de apilar una copia, así que sería un viaje a ninguna
     * parte; y en el tracker, además, recomponer la pantalla de una grabación en curso por un toque
     * sin sentido es lo último que conviene.
     */
    @Test
    fun `tapping the place you are already in goes nowhere`() = runComposeUiTest {
        var travelled = 0
        setContent {
            MovementBottomBar(
                current = MovementPlace.Outings,
                onGoToRecord = { travelled++ },
                onGoToRoutes = { travelled++ },
                onGoToOutings = { travelled++ },
            )
        }

        onNodeWithTag(MovementBottomBarTags.place(MovementPlace.Outings)).performClick()

        assertEquals(0, travelled)
    }
}
