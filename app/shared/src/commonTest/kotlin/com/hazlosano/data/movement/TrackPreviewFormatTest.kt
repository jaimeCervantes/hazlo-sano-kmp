package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Cómo viaja a la base la silueta de una salida, y qué pasa cuando lo que vuelve no se entiende. */
class TrackPreviewFormatTest {

    private fun at(latitude: Double, longitude: Double) =
        UserLocation(latitude = latitude, longitude = longitude, timestamp = 0)

    @Test
    fun `what goes in comes back out`() {
        val points = listOf(at(19.43, -99.13), at(19.4310, -99.132), at(-33.45, 151.21))

        val restored = TrackPreviewFormat.decode(TrackPreviewFormat.encode(points))

        assertEquals(
            points.map { it.latitude to it.longitude },
            restored.map { it.latitude to it.longitude },
        )
    }

    /** Sin puntos no hay silueta, y la columna se queda nula en vez de guardar una cadena vacía. */
    @Test
    fun `no points is stored as nothing at all`() {
        assertNull(TrackPreviewFormat.encode(emptyList()))
    }

    @Test
    fun `nothing stored reads back as no points`() {
        assertTrue(TrackPreviewFormat.decode(null).isEmpty())
        assertTrue(TrackPreviewFormat.decode("").isEmpty())
        assertTrue(TrackPreviewFormat.decode("   ").isEmpty())
    }

    /**
     * La silueta es decoración: una columna a medio escribir no puede costar el historial entero.
     * Se lee lo que se entienda y se descarta el resto, en vez de dejar caer una excepción por una
     * lista que la persona necesita para abrir su salida.
     */
    @Test
    fun `a half-written silhouette keeps the pairs it can read`() {
        val stored = "19.43,-99.13;esto-no-es-un-punto;19.44,-99.14;19.45"

        val restored = TrackPreviewFormat.decode(stored)

        assertEquals(
            listOf(19.43 to -99.13, 19.44 to -99.14),
            restored.map { it.latitude to it.longitude },
        )
    }

    @Test
    fun `something entirely unreadable reads back as no points`() {
        assertTrue(TrackPreviewFormat.decode("vaya;desastre;de;columna").isEmpty())
    }
}
