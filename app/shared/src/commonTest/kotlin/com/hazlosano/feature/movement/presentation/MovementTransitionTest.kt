package com.hazlosano.feature.movement.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Qué transición le toca a cada viaje dentro del pilar de Movimiento.
 *
 * Spec: `features/transiciones_del_pilar.feature`, slice 1.
 *
 * Lo que se afirma es **el criterio**, porque es lo único que puede estar mal: que una pantalla se
 * funda o se deslice se ve mirando, pero que el criterio sea «qué clase de viaje es» y no «cuánto
 * mide la pila» sólo se ve aquí. Es la misma razón por la que `AppLayerTest` afirma el orden de las
 * capas y no cómo se pintan.
 */
class MovementTransitionTest {

    private val tracker = MovementDestination.Tracker()
    private val routes = MovementDestination.Routes
    private val outings = MovementDestination.History
    private val routeDetail = MovementDestination.RouteDetail(routeId = 7)
    private val sessionDetail = MovementDestination.SessionDetail(sessionId = 3)

    /**
     * Entre sitios se funde **pase lo que pase con la pila**, y por eso cada fila se comprueba con
     * los dos valores de `goingDeeper`.
     *
     * Es justo lo que estaba mal: la pila crece al ir a Rutas y mengua al volver a Grabar, así que
     * el mismo toque en la misma barra se animaba hacia un lado o hacia el otro según por dónde
     * hubieras pasado antes.
     */
    @Test
    fun `between places it fades whatever the stack did`() {
        val places = listOf(tracker, routes, outings)

        val journeys = places.flatMap { from ->
            places.filter { it != from }.flatMap { to ->
                listOf(true, false).map { deeper -> Triple(from, to, deeper) }
            }
        }

        journeys.forEach { (from, to, deeper) ->
            assertEquals(
                MovementTransition.Siblings,
                movementTransition(from, to, goingDeeper = deeper),
                "de $from a $to (pila creciendo=$deeper) no se fundió",
            )
        }
    }

    @Test
    fun `opening a detail goes deeper`() {
        assertEquals(
            MovementTransition.Deeper,
            movementTransition(routes, routeDetail, goingDeeper = true),
        )
        assertEquals(
            MovementTransition.Deeper,
            movementTransition(outings, sessionDetail, goingDeeper = true),
        )
    }

    @Test
    fun `coming back from a detail goes back`() {
        assertEquals(
            MovementTransition.Back,
            movementTransition(routeDetail, routes, goingDeeper = false),
        )
        assertEquals(
            MovementTransition.Back,
            movementTransition(sessionDetail, outings, goingDeeper = false),
        )
    }

    /**
     * Empezar una salida desde el detalle de una ruta entra, no se funde.
     *
     * El tracker es un sitio, pero el detalle del que se viene no lo es — y hacen falta los dos para
     * que sea un viaje entre hermanos.
     */
    @Test
    fun `starting an outing from a route detail enters instead of fading`() {
        val following = MovementDestination.Tracker(followRouteId = 7)

        assertEquals(
            MovementTransition.Deeper,
            movementTransition(routeDetail, following, goingDeeper = true),
        )
    }

    /**
     * Cerrar el pilar no es un viaje entre hermanos.
     *
     * `Closed` no se pinta nunca —`lastPlace` conserva la última pantalla mientras el pilar se va—
     * pero el `when` que decide tiene que ser exhaustivo, y que cuente como sitio dejaría el cierre
     * del pilar fundiéndose contra lo que hubiera detrás.
     */
    @Test
    fun `closing the pillar is not a journey between siblings`() {
        assertEquals(
            MovementTransition.Back,
            movementTransition(tracker, MovementDestination.Closed, goingDeeper = false),
        )
    }
}
