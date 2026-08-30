package com.hazlosano.domain.feature.movement.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El desnivel de una ruta y la diferencia entre "no subió" y "no se midió".
 *
 * Es la tercera vez que este pilar tropieza con un dato ausente guardado como cero —la altitud de
 * una lectura, la de un punto al escribir GPX, y ésta—, así que la regla se fija por escrito.
 */
class RouteStatsTest {

    private fun point(lat: Double, lon: Double, altitude: Double? = null) =
        WayPoint(latitude = lat, longitude = lon, altitude = altitude)

    @Test
    fun `a climb measured between two altitudes is reported`() {
        val stats = listOf(
            point(19.4300, -99.1300, altitude = 2_200.0),
            point(19.4310, -99.1300, altitude = 2_220.0),
        ).calculateStats()

        assertEquals(20.0, assertNotNull(stats.second), 0.001)
    }

    @Test
    fun `points with no altitude at all leave the climb unknown`() {
        // Cero afirmaría que la ruta es llana. Nadie comparó nada.
        val stats = listOf(
            point(19.4300, -99.1300),
            point(19.4310, -99.1300),
            point(19.4320, -99.1300),
        ).calculateStats()

        assertNull(stats.second)
    }

    @Test
    fun `ground that really is flat reports zero and not unknown`() {
        val stats = listOf(
            point(19.4300, -99.1300, altitude = 2_200.0),
            point(19.4310, -99.1300, altitude = 2_200.0),
        ).calculateStats()

        assertEquals(0.0, assertNotNull(stats.second), 0.001)
    }

    @Test
    fun `a descent alone is still a measurement so the climb is zero and not unknown`() {
        val stats = listOf(
            point(19.4300, -99.1300, altitude = 2_220.0),
            point(19.4310, -99.1300, altitude = 2_200.0),
        ).calculateStats()

        assertEquals(0.0, assertNotNull(stats.second), 0.001)
    }

    @Test
    fun `one comparable pair among many is enough to have a measurement`() {
        val stats = listOf(
            point(19.4300, -99.1300),
            point(19.4310, -99.1300, altitude = 2_200.0),
            point(19.4320, -99.1300, altitude = 2_215.0),
            point(19.4330, -99.1300),
        ).calculateStats()

        assertEquals(15.0, assertNotNull(stats.second), 0.001)
    }

    @Test
    fun `altitudes that never sit next to each other compare nothing`() {
        // Dos alturas separadas por un punto sin altitud no forman ningún par comparable.
        val stats = listOf(
            point(19.4300, -99.1300, altitude = 2_200.0),
            point(19.4310, -99.1300),
            point(19.4320, -99.1300, altitude = 2_260.0),
        ).calculateStats()

        assertNull(stats.second)
    }

    @Test
    fun `a single point has no climb to measure`() {
        assertNull(listOf(point(19.4300, -99.1300, altitude = 2_200.0)).calculateStats().second)
    }

    @Test
    fun `an empty route measures nothing`() {
        val stats = emptyList<WayPoint>().calculateStats()

        assertEquals(0.0, stats.first, 0.001)
        assertNull(stats.second)
    }

    @Test
    fun `distance is measured whether or not any altitude was`() {
        val stats = listOf(
            point(19.4300, -99.1300),
            point(19.4310, -99.1300),
        ).calculateStats()

        assertTrue(stats.first > 0.0, "la distancia no depende de que haya altitudes")
    }

    @Test
    fun `an unknown climb fingerprints the same as it did before it was nullable`() {
        // La huella usaba `elevationGain.toInt()` sobre el 0.0 que se guardaba. Si cambiara, las
        // rutas ya importadas dejarían de reconocerse y aparecerían como duplicados nuevos.
        val points = listOf(point(19.4300, -99.1300), point(19.4310, -99.1300))
        val unknown = Route(name = "r", distance = 120.0, elevationGain = null, points = points)
        val zero = Route(name = "r", distance = 120.0, elevationGain = 0.0, points = points)

        assertEquals(zero.calculateFingerprint(), unknown.calculateFingerprint())
    }
}
