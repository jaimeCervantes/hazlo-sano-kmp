package com.hazlosano.feature.movement.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Qué destinos del pilar son **un sitio** y cuáles no.
 *
 * Spec: `features/transiciones_del_pilar.feature`, slice 2.
 *
 * Es una sola pregunta de la que salen tres respuestas: qué pestaña se marca abajo, si se pinta la
 * barra de abajo, y si el viaje se funde o entra por el lado. Antes la contestaban dos sitios —el
 * enum encerrado en `MovementBottomBar` y una función de transición aparte— y dos copias de la misma
 * regla se separan en cuanto alguien añade un cuarto destino.
 */
class MovementPlaceTest {

    @Test
    fun `the three places of the pillar are places`() {
        assertEquals(MovementPlace.Record, movementPlaceOf(MovementDestination.Tracker()))
        assertEquals(MovementPlace.Routes, movementPlaceOf(MovementDestination.Routes))
        assertEquals(MovementPlace.Outings, movementPlaceOf(MovementDestination.History))
    }

    /**
     * El tracker sigue siendo el mismo sitio aunque se llegue siguiendo una ruta.
     *
     * Es un destino distinto —la pila no los confunde— pero para el cromo es Grabar: la barra de
     * abajo tiene que marcar lo mismo y la de arriba decir lo mismo.
     */
    @Test
    fun `the tracker is the same place even when it follows a route`() {
        assertEquals(
            MovementPlace.Record,
            movementPlaceOf(MovementDestination.Tracker(followRouteId = 7)),
        )
    }

    /** Un detalle no es un sitio: es algo que abriste, y trae su propio cromo. */
    @Test
    fun `a detail is not a place`() {
        assertNull(movementPlaceOf(MovementDestination.RouteDetail(routeId = 7)))
        assertNull(movementPlaceOf(MovementDestination.SessionDetail(sessionId = 3)))
    }

    /**
     * Y el pilar cerrado tampoco.
     *
     * No se pinta nunca —`lastPlace` conserva la última pantalla mientras el pilar se va— pero que
     * contara como sitio dejaría el cierre del pilar pintando una barra de abajo sobre lo que haya
     * detrás.
     */
    @Test
    fun `the closed pillar is not a place either`() {
        assertNull(movementPlaceOf(MovementDestination.Closed))
    }
}
