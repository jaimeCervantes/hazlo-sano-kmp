package com.hazlosano.domain.feature.movement.model

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A qué distancia estás del trazado de una ruta — del segmento, no de sus vértices. */
class RouteGeometryTest {

    private fun assertMeters(expected: Double, actual: Double?, tolerance: Double = 2.0) {
        assertTrue(actual != null, "no se pudo medir")
        assertTrue(
            abs(expected - actual) <= tolerance,
            "se esperaban ~$expected m y salieron ${actual}",
        )
    }

    @Test
    fun `a point on the track is at no distance from it`() {
        val route = listOf(
            WayPoint(latitude = 19.4300, longitude = -99.1300),
            WayPoint(latitude = 19.4400, longitude = -99.1300),
        )

        assertMeters(0.0, distanceToRouteMeters(19.4350, -99.1300, route), tolerance = 1.0)
    }

    /**
     * El caso que justifica medir contra el segmento y no contra los puntos.
     *
     * Un GPX dibujado a mano puede traer un punto cada kilómetro. Estando en mitad de un tramo recto,
     * por vértices parecerías estar a 500 m de la ruta — cuando estás encima de ella.
     */
    @Test
    fun `the middle of a long straight leg is on the route and not 500 m from its corners`() {
        // Dos puntos separados ~1,1 km de norte a sur.
        val route = listOf(
            WayPoint(latitude = 19.4300, longitude = -99.1300),
            WayPoint(latitude = 19.4400, longitude = -99.1300),
        )

        val fromTrack = distanceToRouteMeters(19.4350, -99.1300, route)

        assertTrue(fromTrack != null && fromTrack < 2.0, "salió $fromTrack m del trazado")
    }

    @Test
    fun `a point beside the track is at its perpendicular distance`() {
        val route = listOf(
            WayPoint(latitude = 19.4300, longitude = -99.1300),
            WayPoint(latitude = 19.4400, longitude = -99.1300),
        )

        // Un grado de longitud a 19.43° son ~105 km, así que 0.000952° son ~100 m.
        assertMeters(100.0, distanceToRouteMeters(19.4350, -99.1300 + 0.000952, route), tolerance = 5.0)
    }

    /** Más allá del final de un tramo, lo más cercano del segmento es su extremo. */
    @Test
    fun `past the end of the track the nearest thing is its end`() {
        val route = listOf(
            WayPoint(latitude = 19.4300, longitude = -99.1300),
            WayPoint(latitude = 19.4310, longitude = -99.1300),
        )

        // ~111 m al norte del final del tramo.
        assertMeters(111.0, distanceToRouteMeters(19.4320, -99.1300, route), tolerance = 5.0)
    }

    @Test
    fun `a route with no points cannot be measured against`() {
        assertNull(distanceToRouteMeters(19.43, -99.13, emptyList()))
    }

    /** Un punto suelto sí se puede medir, aunque no sea un trazado. */
    @Test
    fun `a single point measures as a point`() {
        val route = listOf(WayPoint(latitude = 19.4300, longitude = -99.1300))

        assertMeters(111.0, distanceToRouteMeters(19.4310, -99.1300, route), tolerance = 5.0)
    }

    /** Dos puntos repetidos son un segmento de longitud cero, y no una división por cero. */
    @Test
    fun `a track that repeats a point does not divide by zero`() {
        val route = listOf(
            WayPoint(latitude = 19.4300, longitude = -99.1300),
            WayPoint(latitude = 19.4300, longitude = -99.1300),
        )

        assertMeters(111.0, distanceToRouteMeters(19.4310, -99.1300, route), tolerance = 5.0)
    }

    /**
     * La ruta se recorre entera: el trazado más cercano puede estar en cualquier tramo, no en el
     * primero. Una ruta en forma de L y un punto junto a su segundo brazo.
     */
    @Test
    fun `the nearest leg can be any of them`() {
        val route = listOf(
            WayPoint(latitude = 19.4300, longitude = -99.1300),
            WayPoint(latitude = 19.4400, longitude = -99.1300),
            WayPoint(latitude = 19.4400, longitude = -99.1200),
        )

        // Junto al brazo que va al este, lejos del primero.
        val fromTrack = distanceToRouteMeters(19.4400, -99.1250, route)

        assertTrue(fromTrack != null && fromTrack < 2.0, "salió $fromTrack m del trazado")
    }
}
