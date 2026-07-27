package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.filter.verticalAccuracy
import com.hazlosano.domain.feature.movement.model.Elevation
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.geo.haversineMeters
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Turns the route of a finished session into the figures the app reports about it.
 *
 * Three rules shape everything here:
 *
 * 1. **No threshold is a fixed number of metres.** Nothing tells this code whether the session was a
 *    walk, a jog, a run or a bike ride, and they differ by an order of magnitude in how far a
 *    reading advances. What separates travelling from waiting is therefore speed, not distance.
 * 2. **Altitude is the noisiest thing a GPS reports**, so climb is accumulated with hysteresis
 *    against the quality of each fix instead of adding up every rise between readings.
 * 3. **Nothing measured reads as null, never as zero.** Zero is an assertion — flat ground, no time
 *    spent moving — and a session that carried no altitude never made it.
 *
 * Nothing about a *finished* session is "current": a figure taken from the last few readings
 * describes the moment the user slowed down to press stop, not the outing.
 */
class CalculateStatsUseCase {

    operator fun invoke(points: List<UserLocation>, totalTimeSeconds: Long): SessionStats {
        if (points.size < MINIMUM_POINTS) return SessionStats()

        val measuresAltitude = points.any { it.altitude != 0.0 }

        var elevation = Elevation()
            .accumulating(points.first().altitude, points.first().elevationThreshold())
        var maxAltitude = points.first().altitude
        var minAltitude = points.first().altitude
        // Accumulated as a fraction and rounded once at the end. Truncating each segment to whole
        // seconds threw away most of a second on every one of them, and a session is hundreds of
        // segments long: a real bike ride lost 50 s of 282, which the screen then presented as a
        // pause the rider never took. Synthetic traces could not catch it because their readings
        // land on exact two-second boundaries; real ones never do.
        var movingSeconds = 0.0
        var maxSlope = 0.0
        var totalDistance = 0.0

        points.zipWithNext().forEach { (from, to) ->
            maxAltitude = maxOf(maxAltitude, to.altitude)
            minAltitude = minOf(minAltitude, to.altitude)
            elevation = elevation.accumulating(to.altitude, to.elevationThreshold())

            val meters = haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
            val seconds = (to.timestamp - from.timestamp) / MILLIS_PER_SECOND
            totalDistance += meters

            if (!isTravelling(meters, seconds)) return@forEach
            movingSeconds += seconds

            // Over a short run the altitude error dwarfs the height difference, so the slope it
            // implies is noise rather than terrain.
            if (meters < to.minimumSlopeRun()) return@forEach
            val slope = (to.altitude - from.altitude) / meters * PERCENT
            if (abs(slope) > abs(maxSlope)) maxSlope = slope
        }

        return SessionStats(
            movingTime = movingSeconds.roundToLong(),
            avgPace = paceOver(totalDistance, totalTimeSeconds),
            maxAltitude = maxAltitude.takeIf { measuresAltitude },
            minAltitude = minAltitude.takeIf { measuresAltitude },
            totalAscent = elevation.ascentMeters.takeIf { measuresAltitude },
            totalDescent = elevation.descentMeters.takeIf { measuresAltitude },
            avgSlope = percentageOf(elevation.ascentMeters, totalDistance).takeIf { measuresAltitude },
            maxSlope = maxSlope.takeIf { measuresAltitude },
            vam = verticalSpeedPerHour(elevation.ascentMeters, movingSeconds)
                .takeIf { measuresAltitude },
        )
    }

    /** Distance the way the recording accumulates it: haversine between consecutive points. */
    fun distanceMeters(points: List<UserLocation>): Double =
        points.zipWithNext { from, to ->
            haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
        }.sum()

    /** Slower than any real walk and faster than the drift of a phone lying still. */
    private fun isTravelling(meters: Double, seconds: Double): Boolean =
        seconds > 0.0 && meters / seconds >= MIN_TRAVELLING_SPEED_MPS

    private fun paceOver(meters: Double, seconds: Long): Double? {
        if (meters < MIN_DISTANCE_FOR_PACE_METERS || seconds <= 0L) return null
        return (seconds / SECONDS_PER_MINUTE) / (meters / METERS_PER_KILOMETER)
    }

    private fun verticalSpeedPerHour(ascentMeters: Double, movingSeconds: Double): Double? {
        if (movingSeconds < MIN_SECONDS_FOR_VAM) return null
        return ascentMeters / (movingSeconds / SECONDS_PER_HOUR)
    }

    private fun percentageOf(part: Double, whole: Double): Double? =
        if (whole > 0.0) part / whole * PERCENT else null

    /**
     * How much the altitude has to change before it counts as terrain rather than as error. Derived
     * from the vertical accuracy of the fix that reported it, never from a fixed number of metres.
     */
    private fun UserLocation.elevationThreshold(): Double =
        (verticalAccuracy() * ELEVATION_THRESHOLD_FACTOR)
            .coerceIn(MIN_ELEVATION_THRESHOLD_METERS, MAX_ELEVATION_THRESHOLD_METERS)

    private fun UserLocation.minimumSlopeRun(): Double =
        verticalAccuracy().coerceAtLeast(MIN_SLOPE_RUN_METERS)

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

        const val PERCENT = 100.0
        const val MILLIS_PER_SECOND = 1_000.0
        const val SECONDS_PER_MINUTE = 60.0
        const val SECONDS_PER_HOUR = 3_600.0
        const val METERS_PER_KILOMETER = 1_000.0
    }
}
