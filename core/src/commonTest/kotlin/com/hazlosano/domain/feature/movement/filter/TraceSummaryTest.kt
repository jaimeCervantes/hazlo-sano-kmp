package com.hazlosano.domain.feature.movement.filter

import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The summary is what the session detail shows to explain a distance that came out wrong, so what
 * matters is that it reports the receiver honestly rather than a tidy-looking number.
 */
class TraceSummaryTest {

    @Test
    fun countsWhatArrivedAndWhatBecamePartOfThePath() {
        val records = trace(
            readings = 40,
            metersPerReading = 2.8, // walking
            accuracyMeters = 8f,
            noiseMeters = 4.0,
        ).tracedThrough()

        val summary = records.summarize()

        assertEquals(40, summary.readings)
        assertEquals(summary.readings - summary.accepted, summary.discarded)
        assertTrue(summary.accepted > 0, "a walk accepted nothing at all")
        assertEquals(
            summary.discarded,
            summary.discardedBy.values.sum(),
            "the breakdown by reason must account for every rejected reading",
        )
    }

    @Test
    fun namesTheReasonReadingsWereTurnedAway() {
        // Readings the receiver itself admits are useless: the gate that fires is unambiguous.
        val records = trace(
            readings = 10,
            metersPerReading = 2.8,
            accuracyMeters = 120f,
        ).tracedThrough()

        val summary = records.summarize()

        assertEquals(0, summary.accepted)
        assertEquals(mapOf(DiscardReason.POOR_ACCURACY to 10), summary.discardedBy)
    }

    @Test
    fun reportsHowOftenTheReceiverActuallyDelivered() {
        val records = trace(
            readings = 30,
            metersPerReading = 2.8,
            accuracyMeters = 8f,
        ).tracedThrough()

        val summary = records.summarize()

        assertEquals(SAMPLING_INTERVAL_MILLIS / 1_000.0, summary.samplingIntervalSeconds)
    }

    @Test
    fun aSingleLongGapDoesNotDisguiseTheRateTheSessionRanAt() {
        // Two minutes without a fix in the middle of a session sampled every two seconds. A mean
        // would report roughly six seconds and hide that the rest of the session was healthy.
        val steady = trace(readings = 20, metersPerReading = 2.8, accuracyMeters = 8f)
        val afterTheGap = trace(
            readings = 20,
            metersPerReading = 2.8,
            accuracyMeters = 8f,
            startNorthMeters = 20 * 2.8,
            startAtMillis = steady.last().timestamp + 120_000L,
        )

        val summary = (steady + afterTheGap).tracedThrough().summarize()

        assertEquals(2.0, summary.samplingIntervalSeconds)
    }

    @Test
    fun averagesTheAccuracyTheReceiverReported() {
        val coarse = trace(readings = 10, metersPerReading = 2.8, accuracyMeters = 20f)
        val fine = trace(
            readings = 10,
            metersPerReading = 2.8,
            accuracyMeters = 10f,
            startNorthMeters = 10 * 2.8,
            startAtMillis = coarse.last().timestamp + SAMPLING_INTERVAL_MILLIS,
        )

        val summary = (coarse + fine).tracedThrough().summarize()

        assertTrue(
            abs(summary.averageAccuracyMeters - 15.0) < 0.001,
            "reported ${summary.averageAccuracyMeters} m instead of 15 m",
        )
    }

    @Test
    fun readingsWithoutAnAccuracyAreLeftOutRatherThanCountedAsZero() {
        // Reporting zero here would claim a perfect fix, which is the opposite of what it means.
        val records = listOf(
            TraceRecord(UserLocation(latitude = 19.43, longitude = -99.13, accuracy = 12f), null),
            TraceRecord(UserLocation(latitude = 19.43, longitude = -99.13, accuracy = 0f), null),
        )

        assertEquals(12.0, records.summarize().averageAccuracyMeters)
    }

    @Test
    fun anEmptyTraceSummarizesToNothingInsteadOfBreaking() {
        val summary = emptyList<TraceRecord>().summarize()

        assertEquals(0, summary.readings)
        assertEquals(0, summary.accepted)
        assertEquals(emptyMap(), summary.discardedBy)
        assertEquals(0.0, summary.averageAccuracyMeters)
        assertEquals(0.0, summary.samplingIntervalSeconds)
    }
}
