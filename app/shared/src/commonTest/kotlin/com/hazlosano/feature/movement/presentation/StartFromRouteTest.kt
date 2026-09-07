package com.hazlosano.feature.movement.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Salir a seguir una ruta desde donde se estaba mirando.
 *
 * Es el eslabón que faltaba entre «Mis rutas» y el tracker: hasta ahora, para seguir una ruta había
 * que ir al tracker y buscarla en un diálogo, aunque se viniera de tenerla delante en el mapa.
 *
 * Spec: `features/pulido_de_movimiento.feature`, slice 5.
 */
class StartFromRouteTest {

    /** El camino entero, tal cual lo hace alguien: pilar → rutas → una ruta → Iniciar. */
    private fun atARouteDetail(routeId: Long): MovementNavState = MovementNavState().apply {
        openRoutes()
        openRouteDetail(routeId)
    }

    @Test
    fun `starting from a route carries the route with it`() {
        val nav = atARouteDetail(routeId = 7)

        nav.openTrackerFollowing(routeId = 7)

        val tracker = assertIs<MovementDestination.Tracker>(nav.destination)
        assertEquals(7L, tracker.followRouteId)
    }

    /**
     * Y volver devuelve a la ruta de la que se salió, no a la lista.
     *
     * Es lo que hace que la ida y la vuelta sean simétricas: se salió desde el detalle, así que ahí
     * se vuelve.
     */
    @Test
    fun `going back returns to the route it started from`() {
        val nav = atARouteDetail(routeId = 7)
        nav.openTrackerFollowing(routeId = 7)

        nav.back()

        assertEquals(MovementDestination.RouteDetail(7), nav.destination)
    }

    /**
     * Abrir el tracker a secas y abrirlo con una ruta no son el mismo sitio.
     *
     * La pila corta hasta un destino que ya estaba en vez de apilar una copia, y si los dos fueran
     * iguales, entrar al tracker desde la barra estando en una salida con ruta se comería el detalle
     * de la ruta por el camino.
     */
    @Test
    fun `the plain tracker and the tracker with a route are two places`() {
        val nav = MovementNavState()

        nav.openTracker()
        nav.openTrackerFollowing(routeId = 7)

        val tracker = assertIs<MovementDestination.Tracker>(nav.destination)
        assertEquals(7L, tracker.followRouteId)

        nav.back()

        assertEquals(MovementDestination.Tracker(), nav.destination)
    }

    /** Dos rutas distintas son dos salidas distintas, y no una que se pisa a sí misma. */
    @Test
    fun `starting with another route is another place`() {
        val nav = MovementNavState()

        nav.openTrackerFollowing(routeId = 7)
        nav.openTrackerFollowing(routeId = 8)

        assertEquals(MovementDestination.Tracker(8), nav.destination)

        nav.back()

        assertEquals(MovementDestination.Tracker(7), nav.destination)
    }
}
