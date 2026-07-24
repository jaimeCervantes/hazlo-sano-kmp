package com.hazlosano.feature.movement.presentation

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class MovementFormatTest {

    @Test
    fun showsShortDistancesInMeters() {
        assertEquals("0 m", MovementFormat.distance(0.0))
        assertEquals("450 m", MovementFormat.distance(449.6))
        assertEquals("999 m", MovementFormat.distance(999.4))
    }

    @Test
    fun showsLongDistancesInKilometersWithTwoDecimals() {
        assertEquals("1.00 km", MovementFormat.distance(1_000.0))
        assertEquals("1.25 km", MovementFormat.distance(1_250.0))
        assertEquals("1.05 km", MovementFormat.distance(1_050.0))
        assertEquals("12.30 km", MovementFormat.distance(12_304.0))
    }

    @Test
    fun clampsNegativeDistance() {
        assertEquals("0 m", MovementFormat.distance(-10.0))
    }

    @Test
    fun showsDurationsUnderAnHourAsMinutesAndSeconds() {
        assertEquals("00:00", MovementFormat.duration(0))
        assertEquals("10:00", MovementFormat.duration(600))
        assertEquals("59:59", MovementFormat.duration(3_599))
    }

    @Test
    fun addsHoursToLongerDurations() {
        assertEquals("1:00:00", MovementFormat.duration(3_600))
        assertEquals("2:05:07", MovementFormat.duration(7_507))
    }

    @Test
    fun showsTheSessionDateInTheGivenTimeZone() {
        // 2026-07-24T07:15:00Z
        val epochMillis = 1_784_877_300_000L

        assertEquals("24 jul 2026 · 07:15", MovementFormat.dateTime(epochMillis, TimeZone.UTC))
    }
}
