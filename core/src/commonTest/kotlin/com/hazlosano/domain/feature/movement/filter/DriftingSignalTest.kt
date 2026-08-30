package com.hazlosano.domain.feature.movement.filter

import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.usecase.CalculateStatsUseCase
import com.hazlosano.domain.geo.haversineMeters
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers `features/movement_drifting_signal.feature`.
 *
 * The signal here is the one measured in the field, not the polite one the rest of the suite uses:
 * it strays tens of metres from a phone that never moved and reports a small accuracy while doing
 * it. Every assertion is about what the app would have recorded from it.
 */
class DriftingSignalTest {

    @Test
    fun aPhoneThatDoesNotMoveRecordsNothingHoweverFarTheSignalWanders() {
        val parked = driftingTrace(readings = TWENTY_MINUTES_OF_READINGS)

        val run = parked.through()
        val unfiltered = parked.pathDistanceMeters()

        assertTrue(
            run.distanceMeters < PRACTICALLY_STILL_METERS,
            "a parked phone recorded ${run.distanceMeters} m",
        )
        // Guards the test itself: this is the trace that used to become kilometres.
        assertTrue(
            unfiltered > 1_500.0,
            "the trace is not nasty enough to prove anything: raw $unfiltered m",
        )
    }

    @Test
    fun anExcursionThatComesBackIsNotAJourney() {
        val still = trace(readings = 40, metersPerReading = 0.0, accuracyMeters = 6f, noiseMeters = 3.0)
        // Forty metres away six seconds later: further than the noise floor and within what
        // someone could have covered, so nothing but the wait can tell it from a departure. That
        // is exactly what makes this kind dangerous.
        val excursion = locationAt(
            northMeters = 40.0,
            accuracyMeters = 6f,
            atMillis = still[19].timestamp + 6_000L,
        )

        val run = (still.take(20) + excursion + still.drop(20)).through()

        assertContains(run.discarded, DiscardReason.UNCONFIRMED_MOVEMENT)
        assertTrue(
            DiscardReason.IMPLAUSIBLE_SPEED !in run.discarded,
            "the excursion was caught by the speed gate, so this proves nothing about confirmation",
        )
        assertTrue(run.distanceMeters < 10.0, "the excursion added ${run.distanceMeters} m")
    }

    @Test
    fun movementIsCountedInFullOnceItIsConfirmed() {
        val walkingReadings = 90 // three minutes
        val standing = trace(readings = 30, metersPerReading = 0.0, accuracyMeters = 6f, noiseMeters = 3.0)
        val walking = trace(
            readings = walkingReadings,
            metersPerReading = TravelPace.WALKING.metersPerReading,
            accuracyMeters = 6f,
            noiseMeters = 3.0,
            seed = 7,
            startAtMillis = standing.last().timestamp + SAMPLING_INTERVAL_MILLIS,
        )

        val run = (standing + walking).through()

        // Confirming the departure delays the metres; it does not lose them.
        assertCloseTo(
            expected = travelledMeters(walkingReadings, TravelPace.WALKING.metersPerReading),
            actual = run.distanceMeters,
            tolerance = 0.10,
        )
    }

    @Test
    fun aPauseInTheMiddleOfAnOutingCostsNothingButThePause() {
        val outing = rideThenParkThenRide()

        val recorded = outing.readings.through().distanceMeters

        assertCloseTo(expected = outing.travelledMeters, actual = recorded, tolerance = 0.10)
    }

    @Test
    fun timeSpentNotMovingIsNotReportedAsMovingTime() {
        val outing = rideThenParkThenRide()
        val elapsedSeconds =
            (outing.readings.last().timestamp - outing.readings.first().timestamp) / 1_000L

        val stats = CalculateStatsUseCase()(outing.readings.through().accepted, elapsedSeconds)

        val movingTime = assertNotNull(stats.movingTime, "the outing reported no moving time at all")
        assertTrue(
            movingTime <= outing.ridingSeconds * 6 / 5,
            "$movingTime s of movement reported over ${outing.ridingSeconds} s of riding " +
                "and ${elapsedSeconds - outing.ridingSeconds} s parked",
        )
    }

    @Test
    fun theWanderingReadingsAreCountedUnderTheirOwnReason() {
        val records = driftingTrace(readings = TWENTY_MINUTES_OF_READINGS).tracedThrough()

        val summary = records.summarize()

        assertTrue(
            summary.discardedBy.getValue(DiscardReason.UNCONFIRMED_MOVEMENT) > 0,
            "no reading was reported as unconfirmed movement: ${summary.discardedBy}",
        )
        // Every reading gets a verdict, and only one of them: the diagnosis counts what happened.
        assertTrue(
            records.size == TWENTY_MINUTES_OF_READINGS,
            "${records.size} verdicts for $TWENTY_MINUTES_OF_READINGS readings",
        )
    }

    @Test
    fun theSyntheticSignalWandersTheWayTheMeasuredOneDid() {
        val parked = driftingTrace(readings = TWENTY_MINUTES_OF_READINGS)
        val whereThePhoneReallyIs = locationAt(northMeters = 0.0, accuracyMeters = 6f, atMillis = 0L)

        val strayedBy = parked.associateWith {
            haversineMeters(
                whereThePhoneReallyIs.latitude,
                whereThePhoneReallyIs.longitude,
                it.latitude,
                it.longitude,
            )
        }
        val strayed = strayedBy.values.sorted()
        val ninthDecile = strayed[strayed.size * 9 / 10]
        val netPerMinute = parked.netDisplacementPerMinute().sorted()

        // As far as the field trace strayed (57 m at the ninth decile), and back again (6 m of net
        // displacement per minute). A signal that only did one of the two would not be the one that
        // broke the filter.
        assertTrue(ninthDecile > 40.0, "the signal only strays $ninthDecile m at the ninth decile")
        assertTrue(
            netPerMinute[netPerMinute.size / 2] < 20.0,
            "the signal does not come back: ${netPerMinute[netPerMinute.size / 2]} m of net displacement per minute",
        )
        // And lying about it while it happens, which is the part no synthetic noise had before:
        // readings that claim five metres from thirty metres away.
        assertTrue(
            strayedBy.any { (reading, strayed) -> reading.accuracy <= 6f && strayed > 30.0 },
            "no reading claims to be precise while being nowhere near the phone",
        )
    }

    private data class Outing(
        val readings: List<UserLocation>,
        val travelledMeters: Double,
        val ridingSeconds: Long,
    )

    /** Two rides of two minutes with five minutes parked between them, the signal drifting. */
    private fun rideThenParkThenRide(): Outing {
        val ridingReadings = 60
        val metersPerReading = TravelPace.CYCLING.metersPerReading
        val northAfterRiding = travelledMeters(ridingReadings, metersPerReading)

        val firstRide = trace(
            readings = ridingReadings,
            metersPerReading = metersPerReading,
            accuracyMeters = 6f,
            noiseMeters = 4.0,
            seed = 3,
        )
        val parked = driftingTrace(
            readings = 50, // five minutes at six seconds a reading
            seed = 4,
            startNorthMeters = northAfterRiding,
            startAtMillis = firstRide.last().timestamp + DEGRADED_SAMPLING_INTERVAL_MILLIS,
        )
        val secondRide = trace(
            readings = ridingReadings,
            metersPerReading = metersPerReading,
            accuracyMeters = 6f,
            noiseMeters = 4.0,
            seed = 5,
            startNorthMeters = northAfterRiding,
            startAtMillis = parked.last().timestamp + SAMPLING_INTERVAL_MILLIS,
        )

        return Outing(
            readings = firstRide + parked + secondRide,
            travelledMeters = northAfterRiding * 2,
            ridingSeconds = 2 * ridingReadings * SAMPLING_INTERVAL_MILLIS / 1_000L,
        )
    }

    /** How far the signal actually got from where it was a minute earlier. */
    private fun List<UserLocation>.netDisplacementPerMinute(): List<Double> {
        val displacements = mutableListOf<Double>()
        var windowStart = 0
        forEachIndexed { index, reading ->
            val origin = this[windowStart]
            if (reading.timestamp - origin.timestamp < 60_000L) return@forEachIndexed
            displacements += haversineMeters(
                origin.latitude,
                origin.longitude,
                reading.latitude,
                reading.longitude,
            )
            windowStart = index
        }
        return displacements
    }

    private fun assertCloseTo(expected: Double, actual: Double, tolerance: Double) {
        val drift = abs(actual - expected) / expected
        assertTrue(
            drift <= tolerance,
            "expected about $expected m, recorded $actual m (off by ${(drift * 100).toInt()} %)",
        )
    }

    private companion object {
        /** Two hundred readings at the degraded six-second interval. */
        const val TWENTY_MINUTES_OF_READINGS = 200

        /** Twenty minutes of a wandering signal may not add up to a city block. */
        const val PRACTICALLY_STILL_METERS = 60.0
    }
}
