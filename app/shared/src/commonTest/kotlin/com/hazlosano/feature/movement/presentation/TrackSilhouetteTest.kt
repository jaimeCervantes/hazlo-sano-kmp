package com.hazlosano.feature.movement.presentation

import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * La forma de un recorrido, proyectada en el cuadrado unidad.
 *
 * Se prueba aquí y no en la pantalla porque es una regla: encuadrar, conservar la proporción y
 * corregir la longitud son decisiones que se pueden equivocar, y dentro de un `Canvas` sólo se
 * verían mirando la pantalla con los ojos entrecerrados.
 */
class TrackSilhouetteTest {

    private fun at(latitude: Double, longitude: Double) =
        UserLocation(latitude = latitude, longitude = longitude, timestamp = 0)

    private fun assertClose(expected: Float, actual: Float, what: String) {
        assertTrue(
            abs(expected - actual) < 0.001f,
            "$what: se esperaba ~$expected y salió $actual",
        )
    }

    @Test
    fun `a session with no points has nothing to draw`() {
        assertFalse(emptyList<UserLocation>().toSilhouette().isDrawable)
    }

    /** Un punto es un sitio, no un recorrido. */
    @Test
    fun `a single reading is not a shape`() {
        assertFalse(listOf(at(19.43, -99.13)).toSilhouette().isDrawable)
    }

    /** Todas las lecturas en el mismo sitio: hay puntos, pero no hay por dónde trazar nada. */
    @Test
    fun `a phone that never moved has no shape either`() {
        val standingStill = List(20) { at(19.43, -99.13) }

        assertFalse(standingStill.toSilhouette().isDrawable)
    }

    @Test
    fun `the track is framed inside the unit square`() {
        val silhouette = listOf(
            at(19.4300, -99.1300),
            at(19.4320, -99.1280),
            at(19.4310, -99.1290),
        ).toSilhouette()

        assertTrue(silhouette.isDrawable)
        for (point in silhouette.points) {
            assertTrue(point.x in 0f..1f, "x fuera del cuadro: ${point.x}")
            assertTrue(point.y in 0f..1f, "y fuera del cuadro: ${point.y}")
        }
    }

    /** La latitud crece hacia el norte y la pantalla hacia abajo: el punto más al norte va arriba. */
    @Test
    fun `north is up`() {
        val silhouette = listOf(at(19.4300, -99.1300), at(19.4320, -99.1300)).toSilhouette()

        val (south, north) = silhouette.points
        assertTrue(north.y < south.y, "el punto más al norte no quedó más arriba")
    }

    @Test
    fun `west is left`() {
        val silhouette = listOf(at(19.4300, -99.1300), at(19.4300, -99.1280)).toSilhouette()

        val (west, east) = silhouette.points
        assertTrue(west.x < east.x, "el punto más al oeste no quedó más a la izquierda")
    }

    /**
     * La forma es lo único que esta silueta comunica, así que estirarla hasta llenar la tarjeta sería
     * mentir: un ida y vuelta en línea recta se vería como un circuito. El recorrido se centra en el
     * eje que le sobra.
     */
    @Test
    fun `a track that only goes north stays a vertical line down the middle`() {
        val silhouette = listOf(
            at(19.4300, -99.1300),
            at(19.4310, -99.1300),
            at(19.4320, -99.1300),
        ).toSilhouette()

        for (point in silhouette.points) {
            assertClose(0.5f, point.x, "un recorrido sin anchura debería quedar centrado")
        }
        assertClose(1f, silhouette.points.first().y, "el punto más al sur")
        assertClose(0f, silhouette.points.last().y, "el punto más al norte")
    }

    @Test
    fun `a track that only goes east stays a horizontal line down the middle`() {
        val silhouette = listOf(
            at(19.4300, -99.1300),
            at(19.4300, -99.1280),
        ).toSilhouette()

        for (point in silhouette.points) {
            assertClose(0.5f, point.y, "un recorrido sin altura debería quedar centrado")
        }
        assertClose(0f, silhouette.points.first().x, "el punto más al oeste")
        assertClose(1f, silhouette.points.last().x, "el punto más al este")
    }

    /**
     * Un grado de longitud mide menos que uno de latitud en cuanto te alejas del ecuador. Sin
     * corregirlo, un recorrido cuadrado sobre el terreno saldría más ancho que alto.
     *
     * A 19.43° —Ciudad de México— el coseno vale ~0.943, así que un rectángulo de 0.002° de lado
     * sale más alto que ancho: los dos puntos del este quedan dentro del cuadro, no en su borde.
     */
    @Test
    fun `a degree of longitude is narrower than one of latitude`() {
        val silhouette = listOf(
            at(19.4300, -99.1300),
            at(19.4320, -99.1280),
        ).toSilhouette()

        val (southWest, northEast) = silhouette.points
        assertClose(1f, southWest.y, "el sur debería tocar abajo, que es el eje largo")
        assertClose(0f, northEast.y, "el norte debería tocar arriba")
        assertTrue(
            northEast.x < 1f,
            "la longitud no se corrigió: el este llegó al borde como si midiera lo mismo",
        )
    }

    @Test
    fun `every reading gets a point`() {
        val readings = List(50) { at(19.43 + it * 0.0001, -99.13 + it * 0.0001) }

        assertEquals(50, readings.toSilhouette().points.size)
    }
}
