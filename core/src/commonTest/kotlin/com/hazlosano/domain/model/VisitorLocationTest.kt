package com.hazlosano.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisitorLocationTest {

    @Test
    fun `a real fix is valid`() {
        assertTrue(VisitorLocation(19.4326, -99.1332).isValid)
    }

    @Test
    fun `null island is not a location`() {
        // 0,0 is what an uninitialised struct looks like, not somewhere anyone opens the app from.
        // Sending it would order the whole catalogue by distance to a point in the Atlantic.
        assertFalse(VisitorLocation(0.0, 0.0).isValid)
    }

    @Test
    fun `coordinates outside the earth are rejected`() {
        assertFalse(VisitorLocation(91.0, 0.0).isValid)
        assertFalse(VisitorLocation(0.0, 181.0).isValid)
        assertFalse(VisitorLocation(-90.5, 10.0).isValid)
    }

    @Test
    fun `the poles and the date line are inside the range`() {
        assertTrue(VisitorLocation(90.0, 180.0).isValid)
        assertTrue(VisitorLocation(-90.0, -180.0).isValid)
    }
}
