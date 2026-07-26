package com.hazlosano.domain.feature.movement.filter

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers `features/movement_location_smoothing.feature`.
 *
 * Every case runs a synthetic trace of a known length: the assertions compare what the filter
 * recorded against what was actually travelled, so a change in the maths that happens to keep the
 * two implementations consistent still fails if the recorded distance stops being true.
 */
class LocationFilterTest {

    @Test
    fun standingStillDoesNotAddDistance() {
        val readings = trace(
            readings = 60, // two minutes at a traffic light
            metersPerReading = 0.0,
            accuracyMeters = 10f,
            noiseMeters = 10.0,
        )

        val recorded = readings.through().distanceMeters
        val unfiltered = readings.pathDistanceMeters()

        assertTrue(recorded < 15.0, "standing still recorded $recorded m")
        // Guards the test itself: without the filter this trace does add hundreds of metres.
        assertTrue(
            unfiltered > recorded * 10,
            "the trace is not noisy enough to prove anything: raw $unfiltered m vs filtered $recorded m",
        )
    }

    @Test
    fun theDistanceIsTrueAtEveryPaceTheAppIsUsedAt() {
        // A typical outdoor fix. Walking, jogging, running and cycling all have to come out right
        // from the same code, because nothing ever tells the filter which one is happening.
        val outcomes = TravelPace.entries.map {
            it.recordedOver(minutes = 4, accuracyMeters = 8f, noiseMeters = 5.0)
        }

        val untrustworthy = outcomes.filter { it.drift > CLEAN_SIGNAL_TOLERANCE || it.mistakenForAJump }

        assertTrue(
            untrustworthy.isEmpty(),
            "the recorded distance cannot be trusted at:\n${untrustworthy.joinToString("\n")}",
        )
    }

    @Test
    fun aPoorSignalCostsPrecisionNotTheJourney() {
        // Coarse fixes under tree cover or between buildings. The short paces suffer most: the
        // noise floor scales with the accuracy, so a walker's stride is the first to fall under it.
        val outcomes = TravelPace.entries.map {
            it.recordedOver(minutes = 10, accuracyMeters = 18f, noiseMeters = 15.0)
        }

        val untrustworthy = outcomes.filter { it.drift > POOR_SIGNAL_TOLERANCE || it.mistakenForAJump }

        assertTrue(
            untrustworthy.isEmpty(),
            "a poor signal cost the journey at:\n${untrustworthy.joinToString("\n")}",
        )
    }

    @Test
    fun speedingUpAndSlowingDownWithinOneSession() {
        val metersPerReading = 22.0
        val ridingReadings = 20
        val northAfterRiding = travelledMeters(ridingReadings, metersPerReading)

        val session = trace(
            readings = ridingReadings,
            metersPerReading = metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            seed = 3,
        ) + trace(
            readings = 25, // waiting at a crossing
            metersPerReading = 0.0,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            seed = 4,
            startNorthMeters = northAfterRiding,
            startAtMillis = ridingReadings * SAMPLING_INTERVAL_MILLIS,
        ) + trace(
            readings = ridingReadings,
            metersPerReading = metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            seed = 5,
            startNorthMeters = northAfterRiding,
            startAtMillis = (ridingReadings + 25) * SAMPLING_INTERVAL_MILLIS,
        )

        val recorded = session.through().distanceMeters

        assertCloseTo(northAfterRiding * 2, recorded, tolerance = 0.05)
    }

    @Test
    fun aSingleWildReadingDoesNotCorruptTheSession() {
        val readings = 40
        val metersPerReading = 22.0
        val clean = trace(readings, metersPerReading, accuracyMeters = 8f, noiseMeters = 5.0)
        val beforeTheJump = clean[9]
        val wild = locationAt(
            northMeters = 500.0, // half a kilometre away one second later
            accuracyMeters = 8f,
            atMillis = beforeTheJump.timestamp + 1_000L,
        )

        val withoutIt = clean.through()
        val withIt = (clean.take(10) + wild + clean.drop(10)).through()

        assertContains(withIt.discarded, DiscardReason.IMPLAUSIBLE_SPEED)
        assertCloseTo(travelledMeters(readings, metersPerReading), withIt.distanceMeters, tolerance = 0.05)
        // The session is the same one it would have been: the jump neither added distance nor
        // dragged the estimate towards where the user never was.
        assertEquals(withoutIt.accepted, withIt.accepted)
    }

    @Test
    fun poorQualityReadingsAreNotRecorded() {
        val clean = trace(readings = 20, metersPerReading = 22.0, accuracyMeters = 8f, noiseMeters = 5.0)
        val imprecise = locationAt(
            northMeters = 300.0,
            accuracyMeters = 80f, // "somewhere in this block"
            atMillis = clean[9].timestamp + 1_000L,
        )

        val withoutIt = clean.through()
        val withIt = (clean.take(10) + imprecise + clean.drop(10)).through()

        assertContains(withIt.discarded, DiscardReason.POOR_ACCURACY)
        assertEquals(withoutIt.accepted.size, withIt.accepted.size)
        assertEquals(withoutIt.distanceMeters, withIt.distanceMeters)
    }

    @Test
    fun theFirstLocationStartsThePathWithoutAddingDistance() {
        val run = trace(readings = 1, metersPerReading = 0.0, accuracyMeters = 8f).through()

        assertEquals(1, run.accepted.size)
        assertEquals(0.0, run.distanceMeters)
    }

    @Test
    fun eachRecordingStartsFromACleanSlate() {
        val readings = trace(readings = 30, metersPerReading = 22.0, accuracyMeters = 8f, noiseMeters = 5.0)

        val first = readings.through()
        val second = readings.through()

        // A filter that carried anything over from the previous session would not repeat itself.
        assertEquals(first.distanceMeters, second.distanceMeters)
        assertEquals(first.accepted.size, second.accepted.size)
    }

    @Test
    fun correctsThePositionInsteadOfRecordingTheRawReading() {
        val readings = listOf(
            locationAt(northMeters = 0.0, accuracyMeters = 10f, atMillis = 0L),
            locationAt(northMeters = 40.0, accuracyMeters = 10f, atMillis = SAMPLING_INTERVAL_MILLIS),
        )

        val run = readings.through()

        assertEquals(2, run.accepted.size)
        val moved = run.distanceMeters
        assertTrue(moved in 1.0..39.0, "the reading was stored raw instead of corrected: $moved m")
    }

    private companion object {
        /** The recorded distance has to be true, not merely plausible. */
        const val CLEAN_SIGNAL_TOLERANCE = 0.05

        /** Coarse fixes cost precision; the journey still has to be recognisable. */
        const val POOR_SIGNAL_TOLERANCE = 0.10
    }

    private fun assertCloseTo(expected: Double, actual: Double, tolerance: Double) {
        val drift = abs(actual - expected) / expected
        assertTrue(
            drift <= tolerance,
            "expected about $expected m, recorded $actual m (off by ${(drift * 100).toInt()} %)",
        )
    }
}
