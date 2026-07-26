package com.hazlosano.domain.feature.movement.filter

import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.geo.haversineMeters

/** Why a reading did not make it into the recorded path. */
enum class DiscardReason {
    /** The fix is so imprecise that movement and noise cannot be told apart. */
    POOR_ACCURACY,

    /** The reading implies a speed nobody travels at; the GPS jumped, the user did not. */
    IMPLAUSIBLE_SPEED,

    /** A real reading, but the displacement it shows is smaller than its own uncertainty. */
    WITHIN_NOISE,
}

sealed interface LocationFilterResult {
    /** The filter to use for the next reading; always carries whatever this one taught it. */
    val filter: LocationFilter

    data class Accepted(
        override val filter: LocationFilter,
        val location: UserLocation,
    ) : LocationFilterResult

    data class Discarded(
        override val filter: LocationFilter,
        val reason: DiscardReason,
    ) : LocationFilterResult
}

/**
 * Decides which locations deserve to become part of a recorded session, and corrects the ones that
 * do. Kept apart from what has been recorded: [com.hazlosano.domain.feature.movement.model.RecordingState]
 * answers "what did we record", this answers "is this worth recording".
 *
 * Accepted readings carry the **smoothed** position rather than the raw one, so the drawn path, the
 * accumulated distance and the statistics recomputed from the points all describe the same journey.
 *
 * The thresholds are deliberately expressed against the reading's own accuracy and the time actually
 * elapsed, never as fixed metres. A fixed metre threshold silently encodes an activity: at a two
 * second sampling interval a walk advances under three metres per reading while a bike ride advances
 * tens of them, so any constant tuned for one of them mangles the other.
 */
class LocationFilter private constructor(
    private val smoother: KalmanFilter,
    private val lastAccepted: UserLocation?,
) {

    constructor(smoother: KalmanFilter = KalmanFilter()) : this(smoother, lastAccepted = null)

    fun accepting(location: UserLocation): LocationFilterResult {
        if (location.usableAccuracy() > MAX_USABLE_ACCURACY_METERS) {
            return LocationFilterResult.Discarded(this, DiscardReason.POOR_ACCURACY)
        }

        val previous = lastAccepted
        if (previous != null && impliesImpossibleSpeed(previous, location)) {
            // Deliberately not fed to the smoother: one absurd fix would drag the estimate with it.
            return LocationFilterResult.Discarded(this, DiscardReason.IMPLAUSIBLE_SPEED)
        }

        val smoothing = smoother.smoothing(location)
        val smoothed = smoothing.location
        if (previous == null) return accepted(smoothing.filter, smoothed)

        val moved = haversineMeters(
            previous.latitude,
            previous.longitude,
            smoothed.latitude,
            smoothed.longitude,
        )
        if (moved < noiseFloorMeters(location)) {
            // Still a legitimate reading: it refines the estimate even though the path does not grow.
            // Measuring the next displacement from the last *accepted* point is what lets slow
            // movement add up over several readings instead of being discarded one by one forever.
            return LocationFilterResult.Discarded(
                filter = LocationFilter(smoothing.filter, previous),
                reason = DiscardReason.WITHIN_NOISE,
            )
        }

        return accepted(smoothing.filter, smoothed)
    }

    private fun accepted(smoother: KalmanFilter, location: UserLocation): LocationFilterResult =
        LocationFilterResult.Accepted(LocationFilter(smoother, location), location)

    private fun impliesImpossibleSpeed(previous: UserLocation, location: UserLocation): Boolean {
        val elapsedSeconds = (location.timestamp - previous.timestamp) / MILLIS_PER_SECOND
        // Without a usable interval there is no speed to judge; rejecting on a repeated or backwards
        // timestamp would throw away good readings over a clock problem.
        if (elapsedSeconds <= 0.0) return false

        val apparent = haversineMeters(
            previous.latitude,
            previous.longitude,
            location.latitude,
            location.longitude,
        )
        // Part of that gap is measurement error, not travel: the previous point is a smoothed
        // estimate that lags behind, and this reading carries its own uncertainty. Calling a
        // displacement impossible without discounting it throws away real readings on a fast
        // descent with a coarse signal.
        val travelled = (apparent - location.usableAccuracy()).coerceAtLeast(0.0)
        return travelled / elapsedSeconds > MAX_PLAUSIBLE_SPEED_MPS
    }

    /** Displacement has to beat the uncertainty of the reading that reported it. */
    private fun noiseFloorMeters(location: UserLocation): Double =
        location.usableAccuracy().coerceIn(MIN_NOISE_FLOOR_METERS, MAX_NOISE_FLOOR_METERS)

    private companion object {
        /**
         * 144 km/h. The gate is here to catch the GPS teleporting hundreds of metres, which is two
         * orders of magnitude above this; the headroom over the fastest descent someone might
         * record (~55 km/h) is what keeps a noisy signal from being mistaken for one.
         */
        const val MAX_PLAUSIBLE_SPEED_MPS = 40.0

        /** Beyond this a fix says little more than "somewhere around here". */
        const val MAX_USABLE_ACCURACY_METERS = 50.0

        const val MIN_NOISE_FLOOR_METERS = 3.0
        const val MAX_NOISE_FLOOR_METERS = 20.0

        const val MILLIS_PER_SECOND = 1_000.0
    }
}
