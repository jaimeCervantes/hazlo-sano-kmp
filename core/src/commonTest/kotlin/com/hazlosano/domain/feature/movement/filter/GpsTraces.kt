package com.hazlosano.domain.feature.movement.filter

import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.geo.degreesToRadians
import com.hazlosano.domain.geo.haversineMeters
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
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

/**
 * What the receiver actually delivered once it lost sky: asked for two seconds, it gave a median of
 * six. A provider that slows down is itself part of the failure being reproduced.
 */
internal const val DEGRADED_SAMPLING_INTERVAL_MILLIS = 6_000L

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
    /** False for a receiver that reports a position with no vertical component at all. */
    withAltitude: Boolean = true,
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
            altitudeMeters = if (!withAltitude) {
                null
            } else {
                startAltitudeMeters + index * climbMetersPerReading +
                    random.noise(altitudeNoiseMeters)
            },
        )
    }
}

/**
 * A receiver that has lost sky while the phone does not move: the fix strays tens of metres and
 * comes back, and reports a small accuracy the whole time.
 *
 * Measured on a real phone parked indoors for 31 minutes: consecutive readings a median of 31 m
 * apart while claiming ±6 m, straying 57 m from where the phone was at the ninth decile and 396 m at
 * worst, yet displacing a median of 6 m over any minute. The uniform noise [trace] produces cannot
 * express that — it is small, symmetric and honest about its own accuracy — which is why "standing
 * still adds no distance" passed in this suite while the app recorded 2832 m from a table.
 *
 * The error is a walk pulled back towards where the phone really is, kicked every so often by an
 * excursion. Pulled back is the point: an excursion that never returned would be travel.
 */
internal fun driftingTrace(
    readings: Int,
    /** What a coarse fix claims about itself; in the field, half of them said about this. */
    accuracyMeters: Float = 20f,
    /** And what the occasional one claims — the number the filter used to trust. */
    preciseAccuracyMeters: Float = 5f,
    preciseEveryReadings: Int = 4,
    wanderMeters: Double = 8.0,
    excursionMeters: Double = 95.0,
    excursionEveryReadings: Int = 13,
    /** How much of the error is pulled back on each reading. */
    returnRate: Double = 0.35,
    /** Excursions come home faster than the base wander: in the field they lasted seconds. */
    excursionReturnRate: Double = 0.5,
    intervalMillis: Long = DEGRADED_SAMPLING_INTERVAL_MILLIS,
    seed: Int = 1,
    startNorthMeters: Double = 0.0,
    startAtMillis: Long = 0L,
): List<UserLocation> {
    val random = Random(seed)
    var northError = 0.0
    var eastError = 0.0
    var northExcursion = 0.0
    var eastExcursion = 0.0
    var atMillis = startAtMillis

    return (0 until readings).map { index ->
        if (index > 0) atMillis += intervalMillis
        northError = northError * (1.0 - returnRate) + random.noise(wanderMeters)
        eastError = eastError * (1.0 - returnRate) + random.noise(wanderMeters)
        northExcursion *= (1.0 - excursionReturnRate)
        eastExcursion *= (1.0 - excursionReturnRate)
        if (excursionEveryReadings > 0 && index > 0 && index % excursionEveryReadings == 0) {
            val bearing = random.nextDouble(0.0, 2 * PI)
            northExcursion += excursionMeters * cos(bearing)
            eastExcursion += excursionMeters * sin(bearing)
        }
        locationAt(
            northMeters = startNorthMeters + northError + northExcursion,
            eastMeters = eastError + eastExcursion,
            accuracyMeters = if (preciseEveryReadings > 0 && index % preciseEveryReadings == 0) {
                preciseAccuracyMeters
            } else {
                accuracyMeters
            },
            atMillis = atMillis,
        )
    }
}

internal fun locationAt(
    northMeters: Double,
    eastMeters: Double = 0.0,
    accuracyMeters: Float,
    atMillis: Long,
    altitudeMeters: Double? = BASE_ALTITUDE_METERS,
): UserLocation = UserLocation(
    latitude = BASE_LATITUDE + northMeters / METERS_PER_DEGREE_LATITUDE,
    longitude = BASE_LONGITUDE + eastMeters / metersPerDegreeLongitude(),
    altitude = altitudeMeters,
    accuracy = accuracyMeters,
    timestamp = atMillis,
)

/** Climb the way the previous implementation counted it: every rise between consecutive readings. */
internal fun List<UserLocation>.naiveAscentMeters(): Double =
    zipWithNext { from, to ->
        val climbed = (to.altitude ?: 0.0) - (from.altitude ?: 0.0)
        climbed.coerceAtLeast(0.0)
    }.sum()

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

/**
 * The same run, recorded the way the trace capture records it: every reading with its verdict, each
 * one written when the filter settles it rather than when it arrives.
 */
internal fun List<UserLocation>.tracedThrough(
    start: LocationFilter = LocationFilter(),
): List<TraceRecord> {
    var filter = start
    val records = mutableListOf<TraceRecord>()
    forEach { location ->
        val outcome = filter.accepting(location)
        filter = outcome.filter
        records += outcome.settled
    }
    // A recording that ends mid-departure still owes those readings a verdict.
    return records + filter.closing().settled
}

internal fun List<UserLocation>.through(start: LocationFilter = LocationFilter()): FilterRun {
    var filter = start
    val accepted = mutableListOf<UserLocation>()
    val discarded = mutableListOf<DiscardReason>()

    forEach { location ->
        val outcome = filter.accepting(location)
        filter = outcome.filter
        when (outcome) {
            is LocationFilterResult.Accepted -> accepted += outcome.locations
            is LocationFilterResult.Discarded -> discarded += outcome.reason
        }
    }
    // The readings run out the way a recording stops: whatever is held is judged with what there is.
    accepted += filter.closing().released
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
