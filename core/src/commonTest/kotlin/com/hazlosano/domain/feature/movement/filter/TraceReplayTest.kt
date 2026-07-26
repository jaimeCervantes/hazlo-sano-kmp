package com.hazlosano.domain.feature.movement.filter

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The payoff of capturing a trace: an outing walked once can be run through the filter again, so a
 * threshold can be settled at a desk instead of on the street.
 *
 * These tests are what make the capture worth having. Without them the CSV is a file nobody has
 * checked is usable for the one thing it exists for.
 */
class TraceReplayTest {

    @Test
    fun replayingATraceReproducesTheSessionItCameFrom() {
        val outing = trace(
            readings = 120,
            metersPerReading = 5.0, // jogging
            accuracyMeters = 8f,
            noiseMeters = 5.0,
        )
        val recorded = outing.through().accepted

        val replayed = outing.tracedThrough().map { it.reading }.through().accepted

        assertEquals(recorded, replayed, "the same readings through the same filter drifted apart")
    }

    @Test
    fun aTraceThatKeptOnlyTheAcceptedReadingsWouldNotReproduceAnything() {
        // Why the trace has to hold the rejected readings too, and the reason this is a test and
        // not a comment: a rejected reading still teaches the smoother, so dropping it changes
        // every estimate that follows. A session's stored path is exactly such a trace, which is
        // why the capture could not just read the database back.
        val outing = trace(
            readings = 120,
            metersPerReading = 5.0,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
        )
        val records = outing.tracedThrough()
        val recorded = outing.through().accepted

        val acceptedOnly = records.filter { it.wasAccepted }.map { it.reading }.through().accepted

        assertTrue(
            records.any { !it.wasAccepted },
            "this trace rejected nothing, so it cannot show what dropping the rejected costs",
        )
        assertTrue(
            acceptedOnly != recorded,
            "replaying only the accepted readings happened to match, so the trace proves nothing",
        )
    }

    @Test
    fun everyReadingTheReceiverDeliveredIsInTheTrace() {
        val outing = trace(
            readings = 90,
            metersPerReading = 0.0, // standing still: the case that rejects the most
            accuracyMeters = 10f,
            noiseMeters = 10.0,
        )

        val records = outing.tracedThrough()

        assertEquals(outing.size, records.size)
        assertEquals(outing, records.map { it.reading }, "the trace altered what the receiver said")
    }

    @Test
    fun theReadingsAreKeptRawSoASmoothedPathCanBeRebuiltFromThem() {
        val outing = trace(
            readings = 60,
            metersPerReading = 11.0, // cycling
            accuracyMeters = 8f,
            noiseMeters = 5.0,
        )
        val records = outing.tracedThrough()
        val accepted = outing.through().accepted

        // If the trace had stored the corrected positions, replaying would smooth them twice and
        // the path would keep shrinking every time it was replayed.
        val replayedTwice = records.map { it.reading }.through().accepted.through().accepted

        assertTrue(
            abs(replayedTwice.pathDistanceMeters() - accepted.pathDistanceMeters()) > 1.0,
            "smoothing an already smoothed path changed nothing, so this test cannot fail",
        )
        assertEquals(accepted, records.map { it.reading }.through().accepted)
    }

    @Test
    fun changingAThresholdChangesTheResultWithoutLeavingTheDesk() {
        val outing = trace(
            readings = 120,
            metersPerReading = 2.8, // walking
            accuracyMeters = 12f,
            noiseMeters = 8.0,
        )
        val readings = outing.tracedThrough().map { it.reading }

        val asShipped = readings.through()
        val barelySmoothed = readings.through(
            LocationFilter(KalmanFilter(minProcessNoise = 5.0, maxProcessNoise = 20.0)),
        )

        assertTrue(
            abs(asShipped.distanceMeters - barelySmoothed.distanceMeters) > 1.0,
            "loosening the smoothing left the distance at ${asShipped.distanceMeters} m, so a " +
                "captured trace could not tell one setting from another",
        )
    }
}
