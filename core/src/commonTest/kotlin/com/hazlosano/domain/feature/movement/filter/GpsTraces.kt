package com.hazlosano.domain.feature.movement.filter

import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.geo.degreesToRadians
import com.hazlosano.domain.geo.haversineMeters
import kotlin.math.cos
import kotlin.random.Random

/**
 * Synthetic GPS traces for the filter tests: a journey of a known length with a known amount of
 * noise on top, so an assertion can compare what the filter recorded against what was really
 * travelled instead of against another implementation of the same maths.
 *
 * Noise is uniform in `[-noiseMeters, +noiseMeters]` on each axis and seeded, so a failure is
 * reproducible and identical on every target.
 */

/** Matches the interval `AndroidLocationRepository` asks the fused provider for. */
internal const val SAMPLING_INTERVAL_MILLIS = 2_000L

private const val BASE_LATITUDE = 19.4300
private const val BASE_LONGITUDE = -99.1300
private const val METERS_PER_DEGREE_LATITUDE = 111_320.0

/** Altitude a session starts at unless a test cares about a different one. */
internal const val BASE_ALTITUDE_METERS = 2_000.0

/**
 * A straight journey heading north at a constant pace, optionally gaining height at a constant rate.
 * `metersPerReading = 0.0` is someone standing still while the GPS keeps reporting slightly
 * different positions; `climbMetersPerReading = 0.0` is flat ground.
 *
 * Altitude carries its own noise because the vertical error of a GPS fix is roughly twice the
 * horizontal one, and it is what makes a flat outing report climb it never did.
 */
internal fun trace(
    readings: Int,
    metersPerReading: Double,
    accuracyMeters: Float,
    noiseMeters: Double = 0.0,
    climbMetersPerReading: Double = 0.0,
    altitudeNoiseMeters: Double = 0.0,
    startAltitudeMeters: Double = BASE_ALTITUDE_METERS,
    seed: Int = 1,
    startNorthMeters: Double = 0.0,
    startAtMillis: Long = 0L,
    intervalJitterMillis: Long = 0L,
): List<UserLocation> {
    val random = Random(seed)
    // Drawn from its own generator so adding jitter to a trace does not shift the noise of every
    // existing one.
    val clockJitter = Random(seed + JITTER_SEED_OFFSET)
    var atMillis = startAtMillis

    return (0 until readings).map { index ->
        if (index > 0) atMillis += SAMPLING_INTERVAL_MILLIS + clockJitter.jitter(intervalJitterMillis)
        locationAt(
            northMeters = startNorthMeters + index * metersPerReading + random.noise(noiseMeters),
            eastMeters = random.noise(noiseMeters),
            accuracyMeters = accuracyMeters,
            atMillis = atMillis,
            altitudeMeters = startAltitudeMeters + index * climbMetersPerReading +
                random.noise(altitudeNoiseMeters),
        )
    }
}

internal fun locationAt(
    northMeters: Double,
    eastMeters: Double = 0.0,
    accuracyMeters: Float,
    atMillis: Long,
    altitudeMeters: Double = BASE_ALTITUDE_METERS,
): UserLocation = UserLocation(
    latitude = BASE_LATITUDE + northMeters / METERS_PER_DEGREE_LATITUDE,
    longitude = BASE_LONGITUDE + eastMeters / metersPerDegreeLongitude(),
    altitude = altitudeMeters,
    accuracy = accuracyMeters,
    timestamp = atMillis,
)

/** Climb the way the previous implementation counted it: every rise between consecutive readings. */
internal fun List<UserLocation>.naiveAscentMeters(): Double =
    zipWithNext { from, to -> (to.altitude - from.altitude).coerceAtLeast(0.0) }.sum()

/** What the trace covered in a straight line, ignoring the noise: the number under test. */
internal fun travelledMeters(readings: Int, metersPerReading: Double): Double =
    (readings - 1).coerceAtLeast(0) * metersPerReading

/** Everything the filter let through, plus why it turned the rest away. */
internal data class FilterRun(
    val accepted: List<UserLocation>,
    val discarded: List<DiscardReason>,
) {
    val distanceMeters: Double get() = accepted.pathDistanceMeters()
}

/** The same run, recorded the way the trace capture records it: every reading with its verdict. */
internal fun List<UserLocation>.tracedThrough(
    start: LocationFilter = LocationFilter(),
): List<TraceRecord> {
    var filter = start
    return map { location ->
        val outcome = filter.accepting(location)
        filter = outcome.filter
        TraceRecord(
            reading = location,
            discardReason = (outcome as? LocationFilterResult.Discarded)?.reason,
        )
    }
}

internal fun List<UserLocation>.through(start: LocationFilter = LocationFilter()): FilterRun {
    var filter = start
    val accepted = mutableListOf<UserLocation>()
    val discarded = mutableListOf<DiscardReason>()

    forEach { location ->
        val outcome = filter.accepting(location)
        filter = outcome.filter
        when (outcome) {
            is LocationFilterResult.Accepted -> accepted += outcome.location
            is LocationFilterResult.Discarded -> discarded += outcome.reason
        }
    }
    return FilterRun(accepted, discarded)
}

/** Distance the way the recording accumulates it: haversine between consecutive points. */
internal fun List<UserLocation>.pathDistanceMeters(): Double =
    zipWithNext { from, to ->
        haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude)
    }.sum()

private fun Random.noise(meters: Double): Double =
    if (meters <= 0.0) 0.0 else nextDouble(-meters, meters)

/**
 * A real receiver never delivers on exact interval boundaries. Without this every synthetic reading
 * lands on a whole number of seconds, which hides anything that mishandles the fraction.
 */
private fun Random.jitter(millis: Long): Long =
    if (millis <= 0L) 0L else nextLong(-millis, millis + 1)

private const val JITTER_SEED_OFFSET = 1_000

private fun metersPerDegreeLongitude(): Double =
    METERS_PER_DEGREE_LATITUDE * cos(degreesToRadians(BASE_LATITUDE))
