package com.hazlosano.domain.feature.movement.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeoBoundsTest {

    @Test
    fun hasNoBoundsWhenThereAreNoPoints() {
        assertNull(emptyList<UserLocation>().boundingBox())
    }

    @Test
    fun coversEveryPointOfThePath() {
        val bounds = listOf(
            location(latitude = 19.4300, longitude = -99.1300),
            location(latitude = 19.4500, longitude = -99.1500),
            location(latitude = 19.4100, longitude = -99.1100),
        ).boundingBox()

        assertEquals(
            GeoBounds(
                minLatitude = 19.4100,
                minLongitude = -99.1500,
                maxLatitude = 19.4500,
                maxLongitude = -99.1100,
            ),
            bounds,
        )
    }

    @Test
    fun centersBetweenTheExtremes() {
        val bounds = listOf(
            location(latitude = 19.40, longitude = -99.20),
            location(latitude = 19.50, longitude = -99.10),
        ).boundingBox()!!

        assertEquals(19.45, bounds.centerLatitude, 1e-9)
        assertEquals(-99.15, bounds.centerLongitude, 1e-9)
    }

    @Test
    fun spansAnAreaWhenThePathMoves() {
        val bounds = listOf(
            location(latitude = 19.40, longitude = -99.20),
            location(latitude = 19.41, longitude = -99.20),
        ).boundingBox()!!

        assertTrue(bounds.spansAnArea)
    }

    @Test
    fun spansNoAreaWhenEveryPointIsTheSameCoordinate() {
        val bounds = listOf(
            location(latitude = 19.40, longitude = -99.20),
            location(latitude = 19.40, longitude = -99.20),
        ).boundingBox()!!

        assertFalse(bounds.spansAnArea)
        assertEquals(19.40, bounds.centerLatitude, 1e-9)
        assertEquals(-99.20, bounds.centerLongitude, 1e-9)
    }

    private fun location(latitude: Double, longitude: Double): UserLocation =
        UserLocation(latitude = latitude, longitude = longitude)
}
