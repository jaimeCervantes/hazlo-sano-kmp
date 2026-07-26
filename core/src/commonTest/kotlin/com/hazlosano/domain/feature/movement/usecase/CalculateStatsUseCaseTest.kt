package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.filter.BASE_ALTITUDE_METERS
import com.hazlosano.domain.feature.movement.filter.SAMPLING_INTERVAL_MILLIS
import com.hazlosano.domain.feature.movement.filter.TravelPace
import com.hazlosano.domain.feature.movement.filter.locationAt
import com.hazlosano.domain.feature.movement.filter.naiveAscentMeters
import com.hazlosano.domain.feature.movement.filter.through
import com.hazlosano.domain.feature.movement.filter.trace
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `features/movement_session_statistics.feature`.
 *
 * Traces go through the filter before reaching the statistics, exactly as they do in a recording:
 * measuring the use case on readings the app would never have stored would prove nothing about what
 * the app reports.
 */
class CalculateStatsUseCaseTest {

    @Test
    fun aFlatOutingDoesNotInventClimb() {
        val readings = trace(
            readings = 300, // ten minutes walking on the flat
            metersPerReading = TravelPace.WALKING.metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            climbMetersPerReading = 0.0,
            altitudeNoiseMeters = 15.0,
        )

        val stats = readings.recordedStats()

        val ascent = assertNotNull(stats.totalAscent)
        val descent = assertNotNull(stats.totalDescent)
        assertTrue(ascent < 25.0, "flat ground reported $ascent m of climb")
        assertTrue(descent < 25.0, "flat ground reported $descent m of descent")
        // Guards the test itself: counting every rise, as before, invents hundreds of metres here.
        assertTrue(
            readings.naiveAscentMeters() > ascent * 10,
            "the altitude is not noisy enough to prove anything: " +
                "counting every rise gives ${readings.naiveAscentMeters()} m",
        )
    }

    @Test
    fun aRealClimbIsMeasured() {
        val readings = 300
        val climbPerReading = 0.4 // 120 m over ten minutes
        val trace = trace(
            readings = readings,
            metersPerReading = TravelPace.WALKING.metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            climbMetersPerReading = climbPerReading,
            altitudeNoiseMeters = 15.0,
        )

        val stats = trace.recordedStats()

        assertCloseTo(
            expected = (readings - 1) * climbPerReading,
            actual = stats.totalAscent,
            tolerance = 0.20,
            what = "climb",
        )
    }

    @Test
    fun goingUpAndComingBackDown() {
        val readings = 200
        val climbPerReading = 0.5 // about 100 m up, then the same back down
        val height = (readings - 1) * climbPerReading
        val up = trace(
            readings = readings,
            metersPerReading = TravelPace.WALKING.metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            climbMetersPerReading = climbPerReading,
            altitudeNoiseMeters = 15.0,
            seed = 2,
        )
        val down = trace(
            readings = readings,
            metersPerReading = TravelPace.WALKING.metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            climbMetersPerReading = -climbPerReading,
            altitudeNoiseMeters = 15.0,
            startAltitudeMeters = BASE_ALTITUDE_METERS + height,
            seed = 3,
            startNorthMeters = (readings - 1) * TravelPace.WALKING.metersPerReading,
            startAtMillis = readings * SAMPLING_INTERVAL_MILLIS,
        )

        val stats = (up + down).recordedStats()

        assertCloseTo(height, stats.totalDescent, tolerance = 0.20, what = "descent")
        assertCloseTo(height, stats.totalAscent, tolerance = 0.20, what = "climb")
    }

    @Test
    fun waitingIsNotTimeSpentMoving() {
        val travellingReadings = 90 // three minutes out, three minutes back
        val pauseReadings = 150 // five minutes waiting
        val metersPerReading = TravelPace.RUNNING.metersPerReading
        val northAfterFirstLeg = (travellingReadings - 1) * metersPerReading

        val session = trace(
            readings = travellingReadings,
            metersPerReading = metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            altitudeNoiseMeters = 10.0,
        ) + trace(
            readings = pauseReadings,
            metersPerReading = 0.0,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            altitudeNoiseMeters = 10.0,
            seed = 4,
            startNorthMeters = northAfterFirstLeg,
            startAtMillis = travellingReadings * SAMPLING_INTERVAL_MILLIS,
        ) + trace(
            readings = travellingReadings,
            metersPerReading = metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            seed = 5,
            altitudeNoiseMeters = 10.0,
            startNorthMeters = northAfterFirstLeg,
            startAtMillis = (travellingReadings + pauseReadings) * SAMPLING_INTERVAL_MILLIS,
        )

        val stats = session.recordedStats()

        val travellingSeconds = 2 * travellingReadings * SAMPLING_INTERVAL_MILLIS / 1_000L
        val elapsedSeconds = session.elapsedSeconds()
        val movingTime = assertNotNull(stats.movingTime)
        assertCloseTo(
            expected = travellingSeconds.toDouble(),
            actual = movingTime.toDouble(),
            tolerance = 0.15,
            what = "time spent moving",
        )
        // The pause belongs to the session even though it is not time spent moving.
        assertTrue(
            elapsedSeconds > movingTime + 240,
            "the pause was counted as movement: moving $movingTime s of $elapsedSeconds s",
        )
    }

    @Test
    fun timeSpentMovingIsMeasuredAtEveryPace() {
        val readings = 300
        val wrong = TravelPace.entries.mapNotNull { pace ->
            val trace = trace(
                readings = readings,
                metersPerReading = pace.metersPerReading,
                accuracyMeters = 8f,
                noiseMeters = 5.0,
                altitudeNoiseMeters = 10.0,
            )
            val stats = trace.recordedStats()
            val elapsed = trace.elapsedSeconds()
            val movingTime = assertNotNull(stats.movingTime)
            val covered = movingTime.toDouble() / elapsed
            if (covered >= 0.95) null else "${pace.label}: $movingTime s of $elapsed s moving"
        }

        assertTrue(
            wrong.isEmpty(),
            "travelling without stopping was not counted as movement at:\n${wrong.joinToString("\n")}",
        )
    }

    @Test
    fun theHighestAndLowestPointsReflectTheTerrain() {
        val readings = 150
        val climbPerReading = 0.8
        val height = (readings - 1) * climbPerReading
        val trace = trace(
            readings = readings,
            metersPerReading = TravelPace.WALKING.metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            climbMetersPerReading = climbPerReading,
            altitudeNoiseMeters = 15.0,
        )

        val stats = trace.recordedStats()

        val highest = assertNotNull(stats.maxAltitude)
        val lowest = assertNotNull(stats.minAltitude)
        // What has to be right is the height between the two, not the absolute altitude: a
        // percentage of 2000 m would pass while the whole hill went missing.
        assertCloseTo(
            expected = height,
            actual = highest - lowest,
            tolerance = 0.20,
            what = "height between the lowest and highest points",
        )
        assertTrue(
            abs(lowest - BASE_ALTITUDE_METERS) < ALTITUDE_NOISE_ALLOWANCE_METERS,
            "the lowest point drifted from the terrain: $lowest m",
        )
    }

    @Test
    fun aSessionThatRecordedNoAltitudeReportsNoneRatherThanZero() {
        // Zero would claim flat ground at sea level, which is an assertion the recording never made.
        val readings = trace(
            readings = 120,
            metersPerReading = TravelPace.WALKING.metersPerReading,
            accuracyMeters = 8f,
            noiseMeters = 5.0,
            startAltitudeMeters = 0.0,
        )

        val stats = readings.recordedStats()

        assertNull(stats.totalAscent)
        assertNull(stats.totalDescent)
        assertNull(stats.maxAltitude)
        assertNull(stats.minAltitude)
        // What did not depend on altitude is still measured.
        assertNotNull(stats.movingTime)
        assertNotNull(stats.avgPace)
    }

    @Test
    fun aSessionWithNothingRecordedHasNoStatistics() {
        val nothing = CalculateStatsUseCase()(emptyList(), totalTimeSeconds = 0)
        val onePoint = CalculateStatsUseCase()(
            listOf(locationAt(northMeters = 0.0, accuracyMeters = 8f, atMillis = 0L)),
            totalTimeSeconds = 0,
        )

        // A single position is not a journey: nothing can be measured from it, and reporting an
        // altitude of 2000 m with no climb would read as a real session that went nowhere.
        assertEquals(SessionStats(), nothing)
        assertEquals(SessionStats(), onePoint)
    }

    private companion object {
        /** Residual wander the smoothed altitude is allowed to keep around the real terrain. */
        const val ALTITUDE_NOISE_ALLOWANCE_METERS = 15.0
    }

    /** The path as the app would have stored it, then the figures it would report about it. */
    private fun List<UserLocation>.recordedStats(): SessionStats =
        CalculateStatsUseCase()(through().accepted, elapsedSeconds())

    private fun List<UserLocation>.elapsedSeconds(): Long =
        if (isEmpty()) 0L else (last().timestamp - first().timestamp) / 1_000L

    private fun assertCloseTo(expected: Double, actual: Double?, tolerance: Double, what: String) {
        val measured = assertNotNull(actual, "$what was not measured at all")
        val drift = abs(measured - expected) / expected
        assertTrue(
            drift <= tolerance,
            "$what: expected about $expected, got $measured (off by ${(drift * 100).toInt()} %)",
        )
    }
}
