package com.hazlosano.domain.feature.movement.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Salirse de la ruta, y sobre todo **no decirlo cuando no es verdad**.
 *
 * Spec: [`movement_route_deviation.feature`](../../../../../../../../../features/movement_route_deviation.feature).
 */
class RouteDeviationTest {

    /** Un tramo recto de norte a sur, de algo más de un kilómetro. */
    private val straightRoute = listOf(
        WayPoint(latitude = 19.4300, longitude = -99.1300),
        WayPoint(latitude = 19.4400, longitude = -99.1300),
    )

    private fun at(
        latitude: Double,
        longitude: Double,
        atMillis: Long = 0,
    ) = UserLocation(latitude = latitude, longitude = longitude, timestamp = atMillis)

    /** Un grado de longitud a esta latitud son ~105 km, así que 50 m son ~0.000476°. */
    private fun metersEastOfRoute(meters: Double): Double = -99.1300 + meters / 105_000.0

    @Test
    fun `on the route is on the route`() {
        val deviation = RouteDeviation(straightRoute)

        val standing = deviation.standing(at(19.4350, -99.1300), moving = true)

        assertIs<RouteStanding.OnRoute>(standing)
        assertTrue(standing.metersFromRoute < 1.0, "salió ${standing.metersFromRoute} m")
    }

    /**
     * El peor desvío que las nueve trazas midieron moviéndose fue de 21,3 m. Un umbral que avisara
     * ahí estaría avisando de la imprecisión del receptor, no de un desvío.
     */
    @Test
    fun `the worst stray the field traces measured is not a deviation`() {
        val deviation = RouteDeviation(straightRoute)

        val standing = deviation.standing(
            at(19.4350, metersEastOfRoute(21.3), atMillis = 60_000),
            moving = true,
        )

        assertIs<RouteStanding.OnRoute>(standing)
    }

    /**
     * El desvío que la salida de campo midió, con su forma real — y el que los 30 s originales
     * dejaban pasar en silencio.
     *
     * Se salió del trazado en bici, la señal se alejó progresivamente hasta 77,7 m y volvió: **nueve
     * lecturas por encima de los 50 m repartidas en 16 s**, a lecturas cada 2 s, y 18 s entre las dos
     * lecturas que sí estaban dentro. Es el caso que movió [RouteDeviation.PERSISTENCE_MILLIS] de
     * 30 s a 15 s, así que se queda clavado aquí: si alguien vuelve a subirla, esta prueba lo dice.
     */
    @Test
    fun `the deviation the field outing measured is warned about`() {
        val deviation = RouteDeviation(straightRoute)
        // La rampa medida: cruza los 50 m, sigue alejándose hasta el punto de vuelta.
        val measured = listOf(51.9, 58.9, 64.1, 68.9, 73.6, 76.0, 74.5, 68.6, 58.0)

        val standings = measured.mapIndexed { index, meters ->
            deviation.standing(
                at(19.4350, metersEastOfRoute(meters), atMillis = index * 2_000L),
                moving = true,
            )
        }

        assertIs<RouteStanding.OffRoute>(
            standings.last(),
            "18 s fuera del trazado y llegando a 77 m tienen que avisar",
        )
    }

    @Test
    fun `straying past the tolerance for long enough is a deviation`() {
        val deviation = RouteDeviation(straightRoute)
        val away = metersEastOfRoute(300.0)

        deviation.standing(at(19.4350, away, atMillis = 0), moving = true)
        val standing = deviation.standing(at(19.4350, away, atMillis = 30_000), moving = true)

        assertIs<RouteStanding.OffRoute>(standing)
    }

    /** Una lectura mala suelta no es un desvío: por eso hace falta que aguante. */
    @Test
    fun `a single bad reading is not a deviation`() {
        val deviation = RouteDeviation(straightRoute)
        val away = metersEastOfRoute(300.0)

        deviation.standing(at(19.4350, away, atMillis = 0), moving = true)
        val standing = deviation.standing(at(19.4350, away, atMillis = 5_000), moving = true)

        assertIs<RouteStanding.OnRoute>(standing)
    }

    /**
     * La regla que produjo la cuarta traza de campo: media hora con el teléfono en una mesa coloca
     * la señal hasta a 291 m del sitio donde estaba. Parado, la distancia al trazado es ruido.
     */
    @Test
    fun `standing still is never a deviation however far the signal wanders`() {
        val deviation = RouteDeviation(straightRoute)
        val away = metersEastOfRoute(291.5)

        deviation.standing(at(19.4350, away, atMillis = 0), moving = false)
        val standing = deviation.standing(at(19.4350, away, atMillis = 120_000), moving = false)

        assertEquals(RouteStanding.Unknown, standing)
    }

    /** Y al volver a moverse se empieza a contar de nuevo, sin arrastrar el rato parado. */
    @Test
    fun `time spent standing still does not count towards a deviation`() {
        val deviation = RouteDeviation(straightRoute)
        val away = metersEastOfRoute(300.0)

        deviation.standing(at(19.4350, away, atMillis = 0), moving = false)
        deviation.standing(at(19.4350, away, atMillis = 120_000), moving = false)
        val firstMoving = deviation.standing(at(19.4350, away, atMillis = 121_000), moving = true)

        assertIs<RouteStanding.OnRoute>(firstMoving)
    }

    @Test
    fun `coming back to the route silences the warning at once`() {
        val deviation = RouteDeviation(straightRoute)
        val away = metersEastOfRoute(300.0)
        deviation.standing(at(19.4350, away, atMillis = 0), moving = true)
        assertIs<RouteStanding.OffRoute>(
            deviation.standing(at(19.4350, away, atMillis = 30_000), moving = true),
        )

        val back = deviation.standing(at(19.4350, -99.1300, atMillis = 31_000), moving = true)

        assertIs<RouteStanding.OnRoute>(back)
    }

    /** Y volver a salirse exige aguantar otra vez: la racha anterior no se hereda. */
    @Test
    fun `straying again has to hold on its own`() {
        val deviation = RouteDeviation(straightRoute)
        val away = metersEastOfRoute(300.0)
        deviation.standing(at(19.4350, away, atMillis = 0), moving = true)
        deviation.standing(at(19.4350, away, atMillis = 30_000), moving = true)
        deviation.standing(at(19.4350, -99.1300, atMillis = 31_000), moving = true)

        val strayingAgain = deviation.standing(at(19.4350, away, atMillis = 36_000), moving = true)

        assertIs<RouteStanding.OnRoute>(strayingAgain)
    }

    @Test
    fun `with no route there is no deviation to speak of`() {
        val deviation = RouteDeviation(emptyList())

        val standing = deviation.standing(at(19.9999, -98.0000, atMillis = 60_000), moving = true)

        assertEquals(RouteStanding.Unknown, standing)
    }

    /** Un punto es un sitio, no un trazado: no hay segmento contra el que medir. */
    @Test
    fun `a one-point route is not a track`() {
        val deviation = RouteDeviation(listOf(WayPoint(latitude = 19.43, longitude = -99.13)))

        val standing = deviation.standing(at(19.9999, -98.0000, atMillis = 60_000), moving = true)

        assertEquals(RouteStanding.Unknown, standing)
    }
}
