package com.hazlosano.feature.movement.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Por dónde va el pilar y a dónde vuelve.
 *
 * Lo que estos tests cuidan es que «volver» signifique **lo mismo desde cualquier camino**. Antes
 * cada pantalla decidía a mano a cuál volvía —«Mis salidas» siempre al tracker, «Mis rutas» siempre
 * al historial— y eso coincidía con la verdad sólo mientras la única puerta al pilar fuera el
 * tracker. Desde que se entra también por la pestaña del pilar, dejaba de coincidir.
 */
class MovementNavStateTest {

    @Test
    fun `a pillar nobody opened is closed`() {
        val nav = MovementNavState()

        assertEquals(MovementDestination.Closed, nav.destination)
        assertFalse(nav.isOpen)
    }

    @Test
    fun `going back from the first screen leaves the pillar`() {
        val nav = MovementNavState()
        nav.openTracker()

        nav.back()

        assertEquals(MovementDestination.Closed, nav.destination)
        assertFalse(nav.isOpen)
    }

    /**
     * El camino que motivó la pila: se entra a «Mis rutas» desde la pestaña del pilar, así que
     * volver tiene que salir del pilar — no dejar a alguien en el tracker, donde no ha estado.
     */
    @Test
    fun `routes opened from the pillar go back to the pillar`() {
        val nav = MovementNavState()
        nav.openRoutes()

        nav.back()

        assertEquals(MovementDestination.Closed, nav.destination)
    }

    /** Y el camino de antes sigue funcionando: desde el tracker se vuelve al tracker. */
    @Test
    fun `outings opened from the tracker go back to the tracker`() {
        val nav = MovementNavState()
        nav.openTracker()
        nav.openHistory()

        nav.back()

        assertEquals(MovementDestination.Tracker(), nav.destination)
    }

    @Test
    fun `a session detail goes back to the list it was opened from`() {
        val nav = MovementNavState()
        nav.openHistory()
        nav.openSessionDetail(sessionId = 7)

        nav.back()

        assertEquals(MovementDestination.History, nav.destination)
    }

    @Test
    fun `a route detail goes back to the routes it was opened from`() {
        val nav = MovementNavState()
        nav.openRoutes()
        nav.openRouteDetail(routeId = 3)

        nav.back()

        assertEquals(MovementDestination.Routes, nav.destination)
    }

    /**
     * Ir y venir entre una lista y un detalle no puede hacer crecer la pila: si cada visita apilara,
     * salir del pilar costaría un *volver* por cada una.
     */
    @Test
    fun `revisiting a screen does not stack a second copy of it`() {
        val nav = MovementNavState()
        nav.openRoutes()
        repeat(3) {
            nav.openRouteDetail(routeId = 3)
            nav.openRoutes()
        }

        nav.back()

        assertEquals(MovementDestination.Closed, nav.destination)
    }

    @Test
    fun `closing leaves the pillar however deep you were`() {
        val nav = MovementNavState()
        nav.openTracker()
        nav.openHistory()
        nav.openSessionDetail(sessionId = 1)

        nav.close()

        assertFalse(nav.isOpen)
        assertEquals(MovementDestination.Closed, nav.destination)
    }

    @Test
    fun `two details of different things are two places`() {
        val nav = MovementNavState()
        nav.openRoutes()
        nav.openRouteDetail(routeId = 1)
        nav.openRouteDetail(routeId = 2)

        nav.back()

        assertEquals(MovementDestination.RouteDetail(routeId = 1), nav.destination)
        assertTrue(nav.isOpen)
    }
}
