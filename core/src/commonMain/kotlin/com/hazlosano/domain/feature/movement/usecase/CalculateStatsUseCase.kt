package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.filter.usableVerticalAccuracy
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
 * 4. **An altitude that got stuck asserts nothing about the climb.** A real reading never repeats
 *    its exact value; only a receiver that stopped updating does. Once that has held for long enough
 *    to matter, the whole session's altitude figures fall back to unmeasured rather than mixing a
 *    stuck stretch into a number that looks complete.
 *
 * Nothing about a *finished* session is "current": a figure taken from the last few readings
 * describes the moment the user slowed down to press stop, not the outing.
 */
class CalculateStatsUseCase {

    operator fun invoke(points: List<UserLocation>, totalTimeSeconds: Long): SessionStats {
        if (points.size < MINIMUM_POINTS) return SessionStats()

        // A reading with no altitude reports none, rather than an altitude of zero. Deducing this
        // by comparing against zero, as this used to, cannot tell a missing measurement from a
        // session recorded at sea level.
        val withAltitude = points.mapNotNull { point -> point.altitude?.let { point to it } }
        val measuresAltitude = withAltitude.isNotEmpty() && !withAltitude.hasStaleAltitudeRun()

        var elevation = Elevation()
        var maxAltitude = Double.NEGATIVE_INFINITY
        var minAltitude = Double.POSITIVE_INFINITY
        withAltitude.forEach { (point, altitude) ->
            maxAltitude = maxOf(maxAltitude, altitude)
            minAltitude = minOf(minAltitude, altitude)
            elevation = elevation.accumulating(altitude, point.elevationThreshold())
        }
        // Accumulated as a fraction and rounded once at the end. Truncating each segment to whole
        // seconds threw away most of a second on every one of them, and a session is hundreds of
        // segments long: a real bike ride lost 50 s of 282, which the screen then presented as a
        // pause the rider never took. Synthetic traces could not catch it because their readings
        // land on exact two-second boundaries; real ones never do.
        var movingSeconds = 0.0
        var maxSlope = 0.0
        var totalDistance = 0.0

        points.zipWithNext().forEach { (from, to) ->
            val meters = haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
            val seconds = (to.timestamp - from.timestamp) / MILLIS_PER_SECOND
            totalDistance += meters

            if (!isTravelling(meters, seconds)) return@forEach
            movingSeconds += seconds

            // Over a short run the altitude error dwarfs the height difference, so the slope it
            // implies is noise rather than terrain. A pair without altitudes has no slope at all.
            if (meters < to.minimumSlopeRun()) return@forEach
            val climbed = (to.altitude ?: return@forEach) - (from.altitude ?: return@forEach)
            val slope = climbed / meters * PERCENT
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
        (usableVerticalAccuracy() * ELEVATION_THRESHOLD_FACTOR)
            .coerceIn(MIN_ELEVATION_THRESHOLD_METERS, MAX_ELEVATION_THRESHOLD_METERS)

    private fun UserLocation.minimumSlopeRun(): Double =
        usableVerticalAccuracy().coerceAtLeast(MIN_SLOPE_RUN_METERS)

    /**
     * Whether the altitude held its exact value for long enough to be a stuck receiver rather than
     * genuinely flat terrain.
     *
     * The signal is duration, not how many readings repeat: the sampling interval is not constant,
     * so counting readings would answer a different question on a session with a degraded fix than
     * on a healthy one. A run's span is measured between its first and last identical reading, which
     * is exactly what a two-reading run already expresses without needing anything in between.
     */
    private fun List<Pair<UserLocation, Double>>.hasStaleAltitudeRun(): Boolean {
        if (size < 2) return false

        var runStartMillis = this[0].first.timestamp
        for (index in 1 until size) {
            val previousAltitude = this[index - 1].second
            val (point, altitude) = this[index]
            if (altitude != previousAltitude) {
                runStartMillis = point.timestamp
                continue
            }
            if (point.timestamp - runStartMillis >= STALE_ALTITUDE_RUN_MILLIS) return true
        }
        return false
    }

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

        /**
         * How long an unchanged altitude has to hold before it is treated as a stuck receiver rather
         * than flat ground. A conservative starting point: well under the 385 s freeze measured in
         * the field, with no real trace on record showing a short, legitimate repeat. Flagged as the
         * number to revisit once another field trace is captured.
         */
        const val STALE_ALTITUDE_RUN_MILLIS = 60_000L

        const val PERCENT = 100.0
        const val MILLIS_PER_SECOND = 1_000.0
        const val SECONDS_PER_MINUTE = 60.0
        const val SECONDS_PER_HOUR = 3_600.0
        const val METERS_PER_KILOMETER = 1_000.0
    }
}
