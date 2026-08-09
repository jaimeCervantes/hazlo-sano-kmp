package com.hazlosano.domain.feature.movement.filter

import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.geo.haversineMeters

/**
 * Adaptive Kalman smoother for GPS readings: position and altitude.
 *
 * The app never knows whether the session being recorded is a walk, a jog, a run or a bike ride, and
 * they differ by an order of magnitude in metres per reading. So the amount of horizontal smoothing
 * is not a constant: the faster the observed movement, the more the filter trusts the raw
 * measurement and the less it corrects it. Over-smoothing someone on a bike rounds off every curve
 * and loses distance; under-smoothing someone standing still turns noise into metres.
 *
 * The observed speed is measured **after discounting the reading's own accuracy**. Without that,
 * ten metres of noise around a standing user reads as five metres per second and the filter stops
 * smoothing exactly when smoothing matters most.
 *
 * Altitude is smoothed separately and **not** adaptively, because the same trick cannot work
 * vertically: real climb advances a few tens of centimetres between readings while the vertical
 * error is measured in tens of metres, so there is no per-reading vertical rate to detect. Instead
 * it assumes a plausible rate of real climb and lets the filter reject everything faster than that.
 *
 * Immutable on purpose: each reading yields a new filter, so a recording can hold one as part of
 * its state and no session can inherit the estimate left by the previous one.
 */
class KalmanFilter private constructor(
    private val estimate: Estimate?,
    private val minProcessNoise: Double,
    private val maxProcessNoise: Double,
) {

    constructor(
        minProcessNoise: Double = MIN_PROCESS_NOISE_MPS,
        maxProcessNoise: Double = MAX_PROCESS_NOISE_MPS,
    ) : this(estimate = null, minProcessNoise = minProcessNoise, maxProcessNoise = maxProcessNoise)

    /** The filter after taking a reading in, together with the position it corrected it to. */
    data class Smoothing(val filter: KalmanFilter, val location: UserLocation)

    fun smoothing(location: UserLocation): Smoothing {
        val measurementVariance = location.usableAccuracy().let { it * it }
        val previous = estimate
            ?: return smoothingAt(
                location = location,
                estimate = Estimate(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    variance = measurementVariance,
                    altitude = location.altitude,
                    altitudeVariance = location.verticalVariance(),
                    atMillis = location.timestamp,
                ),
            )

        val elapsedSeconds = (location.timestamp - previous.atMillis).coerceAtLeast(0L) / MILLIS_PER_SECOND

        val predictedVariance = previous.variance + elapsedSeconds * predictedDrift(previous, location, elapsedSeconds)
        // Kalman gain: how much of the difference between measurement and estimate we adopt.
        val gain = predictedVariance / (predictedVariance + measurementVariance)

        val vertical = smoothedAltitude(previous, location, elapsedSeconds)

        return smoothingAt(
            location = location,
            estimate = Estimate(
                latitude = previous.latitude + gain * (location.latitude - previous.latitude),
                longitude = previous.longitude + gain * (location.longitude - previous.longitude),
                variance = (1.0 - gain) * predictedVariance,
                altitude = vertical.first,
                altitudeVariance = vertical.second,
                atMillis = maxOf(previous.atMillis, location.timestamp),
            ),
        )
    }

    /**
     * A reading with no altitude leaves the vertical estimate untouched rather than dragging it
     * anywhere: there is nothing to learn from it. The first reading that does carry one starts the
     * estimate from that measurement instead of smoothing it against nothing.
     */
    private fun smoothedAltitude(
        previous: Estimate,
        location: UserLocation,
        elapsedSeconds: Double,
    ): Pair<Double?, Double> {
        val measured = location.altitude ?: return previous.altitude to previous.altitudeVariance
        val measurementVariance = location.verticalVariance()
        val previousAltitude = previous.altitude ?: return measured to measurementVariance

        val predicted = previous.altitudeVariance + elapsedSeconds * VERTICAL_DRIFT_MPS * VERTICAL_DRIFT_MPS
        val altitudeGain = predicted / (predicted + measurementVariance)
        return previousAltitude + altitudeGain * (measured - previousAltitude) to
            (1.0 - altitudeGain) * predicted
    }

    private fun smoothingAt(location: UserLocation, estimate: Estimate): Smoothing =
        Smoothing(
            filter = KalmanFilter(estimate, minProcessNoise, maxProcessNoise),
            location = location.copy(
                latitude = estimate.latitude,
                longitude = estimate.longitude,
                // A reading that carried no altitude keeps none. Handing back the last estimate
                // would dress a stale altitude up as a fresh measurement, which is the exact
                // failure this slice exists to stop hiding.
                altitude = if (location.altitude == null) null else estimate.altitude,
            ),
        )

    /** How far the position may plausibly have drifted per second, given the movement observed. */
    private fun predictedDrift(
        previous: Estimate,
        location: UserLocation,
        elapsedSeconds: Double,
    ): Double {
        if (elapsedSeconds <= 0.0) return 0.0

        val apparent = haversineMeters(previous.latitude, previous.longitude, location.latitude, location.longitude)
        val moved = (apparent - location.usableAccuracy()).coerceAtLeast(0.0)
        val speed = (moved / elapsedSeconds).coerceIn(minProcessNoise, maxProcessNoise)
        return speed * speed
    }

    private data class Estimate(
        val latitude: Double,
        val longitude: Double,
        val variance: Double,
        /** Null until a reading has actually reported an altitude. */
        val altitude: Double?,
        val altitudeVariance: Double,
        val atMillis: Long,
    )

    private companion object {
        /** Walking pace still needs real smoothing, so the floor stays low. */
        const val MIN_PROCESS_NOISE_MPS = 1.0

        /** Above this the reading is handled by the plausibility gate, not by trusting it more. */
        const val MAX_PROCESS_NOISE_MPS = 25.0

        /**
         * Balances lag against noise, and is deliberately loose: smoothing the altitude hard enough
         * to reject the noise on its own leaves the estimate tens of metres behind a real climb, so
         * the summit is measured too low and the descent that follows starts truncated. Rejecting
         * the noise is the job of the hysteresis that accumulates the climb, downstream of here;
         * this only has to take the edge off each reading.
         */
        const val VERTICAL_DRIFT_MPS = 1.0

        const val MILLIS_PER_SECOND = 1_000.0
    }
}

/**
 * Providers normally report accuracy, but a reading without one must not collapse the maths
 * (a zero measurement variance makes the Kalman gain undefined), so it is treated as a typical fix.
 */
internal fun UserLocation.usableAccuracy(): Double =
    if (accuracy > 0f) accuracy.toDouble() else ASSUMED_ACCURACY_METERS

/**
 * What the reading itself says about its altitude, when it says anything.
 *
 * When it does not, satellite geometry puts the receiver on one side of the sky rather than
 * surrounding it, so the vertical error of a fix runs about twice its horizontal one. That estimate
 * is a fallback for receivers that report no vertical accuracy, not a substitute for asking.
 */
internal fun UserLocation.usableVerticalAccuracy(): Double =
    verticalAccuracy?.takeIf { it > 0f }?.toDouble() ?: (usableAccuracy() * VERTICAL_ACCURACY_RATIO)

private fun UserLocation.verticalVariance(): Double =
    usableVerticalAccuracy().let { it * it }

internal const val ASSUMED_ACCURACY_METERS = 10.0
internal const val VERTICAL_ACCURACY_RATIO = 2.0
