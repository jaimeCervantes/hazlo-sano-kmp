package com.hazlosano.domain.feature.movement.filter

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The paces this app is actually used at. They span an order of magnitude in metres per reading,
 * which is precisely why no threshold in the filter may be a fixed number of metres: the app has no
 * activity selector, so the same code has to serve a walk and a descent on a bike.
 */
internal enum class TravelPace(val label: String, private val kilometersPerHour: Double) {
    WALKING("walking", 5.0),
    JOGGING("jogging", 9.0),
    RUNNING("running", 14.0),
    CYCLING("cycling", 25.0),
    CYCLING_FAST("cycling fast", 40.0),
    CYCLING_DOWNHILL("cycling downhill", 55.0),
    ;

    val metersPerReading: Double
        get() = kilometersPerHour / SECONDS_PER_HOUR * METERS_PER_KILOMETER * (SAMPLING_INTERVAL_MILLIS / MILLIS_PER_SECOND)

    private companion object {
        const val SECONDS_PER_HOUR = 3_600.0
        const val METERS_PER_KILOMETER = 1_000.0
        const val MILLIS_PER_SECOND = 1_000.0
    }
}

/** What one pace covered against what the filter recorded of it. */
internal data class PaceOutcome(
    val pace: TravelPace,
    val travelledMeters: Double,
    val recordedMeters: Double,
    val readings: Int,
    val readingsCalledJumps: Int,
) {
    val drift: Double get() = abs(recordedMeters - travelledMeters) / travelledMeters

    /**
     * A noise spike that lands far enough away is genuinely something nobody could have done, and
     * turning it away is the filter working. What may never happen is a journey being read as a
     * string of teleports, so this is a share and not a single occurrence.
     */
    val mistakenForAJump: Boolean get() = readingsCalledJumps > readings * MAX_JUMP_SHARE

    override fun toString(): String =
        "${pace.label}: travelled ${travelledMeters.oneDecimal()} m, " +
            "recorded ${recordedMeters.oneDecimal()} m " +
            "(off by ${(drift * 100).oneDecimal()} %)" +
            if (mistakenForAJump) {
                " — $readingsCalledJumps OF $readings READINGS DISCARDED AS IMPOSSIBLE JUMPS"
            } else {
                ""
            }
}

private const val MAX_JUMP_SHARE = 0.05

/** Runs a straight journey at [pace] through the filter and reports how it fared. */
internal fun TravelPace.recordedOver(
    minutes: Int,
    accuracyMeters: Float,
    noiseMeters: Double,
    seed: Int = 1,
): PaceOutcome {
    val readings = (minutes * 60_000L / SAMPLING_INTERVAL_MILLIS).toInt()
    val run = trace(
        readings = readings,
        metersPerReading = metersPerReading,
        accuracyMeters = accuracyMeters,
        noiseMeters = noiseMeters,
        seed = seed,
    ).through()

    return PaceOutcome(
        pace = this,
        travelledMeters = travelledMeters(readings, metersPerReading),
        recordedMeters = run.distanceMeters,
        readings = readings,
        readingsCalledJumps = run.discarded.count { it == DiscardReason.IMPLAUSIBLE_SPEED },
    )
}

private fun Double.oneDecimal(): String = ((this * 10).roundToInt() / 10.0).toString()
