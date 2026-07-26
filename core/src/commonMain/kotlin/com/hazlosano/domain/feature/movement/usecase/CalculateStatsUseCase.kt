package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.filter.verticalAccuracy
import com.hazlosano.domain.feature.movement.model.Elevation
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.geo.haversineMeters
import kotlin.math.abs
import kotlin.math.floor

/**
 * Turns the path of a finished session into the figures the app reports about it.
 *
 * Two rules shape everything here, both learned from the recorded distance:
 *
 * 1. **No threshold is a fixed number of metres.** Nothing tells this code whether the session was a
 *    walk, a jog, a run or a bike ride, and they differ by an order of magnitude in how far a
 *    reading advances. What separates travelling from waiting is therefore speed, not distance.
 * 2. **Altitude is the noisiest thing a GPS reports**, so climb is accumulated with hysteresis
 *    against the quality of each fix instead of adding up every rise between readings.
 */
class CalculateStatsUseCase {

    operator fun invoke(points: List<UserLocation>, totalTimeSeconds: Long): SessionStats {
        if (points.size < MINIMUM_POINTS) return SessionStats()

        var elevation = Elevation().accumulating(points.first().altitude, points.first().elevationThreshold())
        var maxAltitude = points.first().altitude
        var minAltitude = points.first().altitude
        var movingSeconds = 0L
        var maxSlope = 0.0
        var currentSlope = 0.0
        var totalDistance = 0.0
        val timePerAltitudeBand = mutableMapOf<Int, Long>()

        points.zipWithNext().forEachIndexed { index, (from, to) ->
            maxAltitude = maxOf(maxAltitude, to.altitude)
            minAltitude = minOf(minAltitude, to.altitude)
            elevation = elevation.accumulating(to.altitude, to.elevationThreshold())

            val meters = haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
            val seconds = (to.timestamp - from.timestamp) / MILLIS_PER_SECOND
            totalDistance += meters

            val band = altitudeBandOf(from.altitude)
            timePerAltitudeBand[band] = (timePerAltitudeBand[band] ?: 0L) + seconds.toLong()

            if (!isTravelling(meters, seconds)) return@forEachIndexed
            movingSeconds += seconds.toLong()

            // Over a short run the altitude error dwarfs the height difference, so the slope it
            // implies is noise rather than terrain.
            if (meters < to.minimumSlopeRun()) return@forEachIndexed
            val slope = (to.altitude - from.altitude) / meters * PERCENT
            if (abs(slope) > abs(maxSlope)) maxSlope = slope
            if (index == points.size - 2) currentSlope = slope
        }

        return SessionStats(
            maxAltitude = maxAltitude,
            minAltitude = minAltitude,
            totalAscent = elevation.ascentMeters,
            totalDescent = elevation.descentMeters,
            avgSlope = percentageOf(elevation.ascentMeters, totalDistance),
            maxSlope = maxSlope,
            currentSlope = currentSlope,
            movingTime = movingSeconds,
            vam = verticalSpeedPerHour(elevation.ascentMeters, movingSeconds),
            currentPace = paceOf(points.takeLast(POINTS_FOR_CURRENT_PACE)),
            avgPace = paceOver(totalDistance, totalTimeSeconds),
            altitudeDistribution = timePerAltitudeBand,
        )
    }

    /** Slower than any real walk and faster than the drift of a phone lying still. */
    private fun isTravelling(meters: Double, seconds: Double): Boolean =
        seconds > 0.0 && meters / seconds >= MIN_TRAVELLING_SPEED_MPS

    private fun paceOf(recentPoints: List<UserLocation>): Double {
        if (recentPoints.size < POINTS_FOR_CURRENT_PACE) return NO_VALUE

        val meters = recentPoints.zipWithNext { from, to ->
            haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
        }.sum()
        val seconds = (recentPoints.last().timestamp - recentPoints.first().timestamp) / MILLIS_PER_SECOND
        if (!isTravelling(meters, seconds)) return NO_VALUE

        return paceOver(meters, seconds.toLong())
    }

    private fun paceOver(meters: Double, seconds: Long): Double {
        if (meters < MIN_DISTANCE_FOR_PACE_METERS || seconds <= 0L) return NO_VALUE
        return (seconds / SECONDS_PER_MINUTE) / (meters / METERS_PER_KILOMETER)
    }

    private fun verticalSpeedPerHour(ascentMeters: Double, movingSeconds: Long): Double {
        if (movingSeconds < MIN_SECONDS_FOR_VAM) return NO_VALUE
        return ascentMeters / (movingSeconds / SECONDS_PER_HOUR)
    }

    private fun percentageOf(part: Double, whole: Double): Double =
        if (whole > 0.0) part / whole * PERCENT else NO_VALUE

    /**
     * How much the altitude has to change before it counts as terrain rather than as error. Derived
     * from the vertical accuracy of the fix that reported it, never from a fixed number of metres.
     */
    private fun UserLocation.elevationThreshold(): Double =
        (verticalAccuracy() * ELEVATION_THRESHOLD_FACTOR)
            .coerceIn(MIN_ELEVATION_THRESHOLD_METERS, MAX_ELEVATION_THRESHOLD_METERS)

    private fun UserLocation.minimumSlopeRun(): Double =
        verticalAccuracy().coerceAtLeast(MIN_SLOPE_RUN_METERS)

    private fun altitudeBandOf(altitudeMeters: Double): Int =
        (floor(altitudeMeters / ALTITUDE_BAND_METERS) * ALTITUDE_BAND_METERS).toInt()

    private companion object {
        /** One point is a position, not a journey: nothing can be measured from it. */
        const val MINIMUM_POINTS = 2

        const val MIN_TRAVELLING_SPEED_MPS = 0.5
        const val ELEVATION_THRESHOLD_FACTOR = 0.6
        const val MIN_ELEVATION_THRESHOLD_METERS = 3.0
        const val MAX_ELEVATION_THRESHOLD_METERS = 12.0
        const val MIN_SLOPE_RUN_METERS = 20.0
        const val MIN_DISTANCE_FOR_PACE_METERS = 10.0
        const val MIN_SECONDS_FOR_VAM = 30L
        const val POINTS_FOR_CURRENT_PACE = 5
        const val ALTITUDE_BAND_METERS = 500.0

        const val NO_VALUE = 0.0
        const val PERCENT = 100.0
        const val MILLIS_PER_SECOND = 1_000.0
        const val SECONDS_PER_MINUTE = 60.0
        const val SECONDS_PER_HOUR = 3_600.0
        const val METERS_PER_KILOMETER = 1_000.0
    }
}
