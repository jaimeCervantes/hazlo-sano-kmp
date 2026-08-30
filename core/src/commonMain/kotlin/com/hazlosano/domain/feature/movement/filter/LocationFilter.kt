package com.hazlosano.domain.feature.movement.filter

import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.geo.haversineMeters

/** Why a reading did not make it into the recorded path. */
enum class DiscardReason {
    /** The fix is so imprecise that movement and noise cannot be told apart. */
    POOR_ACCURACY,

    /** The reading implies a journey nobody could have made; the GPS jumped, the user did not. */
    IMPLAUSIBLE_SPEED,

    /** A real reading, but the displacement it shows is smaller than its own uncertainty. */
    WITHIN_NOISE,

    /**
     * The reading left the recorded path behind, but the departure never held up: a minute later
     * the receiver had got nowhere. A phone on a table produces hundreds of these, and every one of
     * them used to be metres.
     */
    UNCONFIRMED_MOVEMENT,
}

sealed interface LocationFilterResult {
    /** The filter to use for the next reading; always carries whatever this one taught it. */
    val filter: LocationFilter

    /**
     * The readings whose verdict is final as of this one, oldest first, exactly as the receiver
     * delivered them.
     *
     * A departure is judged after the fact, so the verdict for a reading is not always known when it
     * arrives. This is what a trace has to record, and in this order: writing the verdict a reading
     * got on arrival would report readings as unconfirmed that ended up in the path, and writing
     * them out of order would produce a trace that no longer replays into the session it came from.
     */
    val settled: List<TraceRecord>

    data class Accepted(
        override val filter: LocationFilter,
        /**
         * Everything that just became part of the path, oldest first — normally one reading, and
         * the whole held departure when this reading is the one that confirmed it.
         */
        val locations: List<UserLocation>,
        override val settled: List<TraceRecord>,
    ) : LocationFilterResult

    data class Discarded(
        override val filter: LocationFilter,
        val reason: DiscardReason,
        override val settled: List<TraceRecord>,
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
 *
 * **What a reading claims about itself is not evidence.** Measured in the field: a phone parked
 * indoors for half an hour delivered fixes claiming ±3 m from 170 m away, sat there for a minute and
 * came back, and the app would have recorded 2832 m from a table. So a departure from the path is
 * not distance until it holds up — the filter asks where the receiver **has been** over the recent
 * half of its window against where it **had been** over the half before, each taken as a middle
 * reading so that one excursion cannot answer for the whole of it. Until that can be answered the
 * reading is held, not thrown away, and confirming releases everything held, so the length of the
 * path survives the wait. Once travel is established each reading is released as it arrives and the
 * distance is live; only the first minute, and the minute after each stop, arrive together.
 */
class LocationFilter private constructor(
    private val smoother: KalmanFilter,
    /** The last point released into the path; null until the first reading arrives. */
    private val anchor: UserLocation?,
    /**
     * The last window of smoothed positions, released or not: the memory that answers where the
     * receiver has been, which is the only evidence that tells travel from a wandering signal.
     */
    private val trail: List<UserLocation>,
    /** Readings whose verdict cannot be written yet, in the order the receiver delivered them. */
    private val waiting: List<Waiting>,
    /** When the receiver last left the anchor behind, or null while it is still around it. */
    private val departedAtMillis: Long?,
    /** Until when travel counts as established, so readings need no further confirmation. */
    private val travellingUntilMillis: Long?,
) {

    constructor(smoother: KalmanFilter = KalmanFilter()) : this(
        smoother = smoother,
        anchor = null,
        trail = emptyList(),
        waiting = emptyList(),
        departedAtMillis = null,
        travellingUntilMillis = null,
    )

    /**
     * A reading whose verdict cannot be written yet, either because its departure has not been
     * judged or because an older one has not.
     *
     * A reading turned away on its own merits waits its turn rather than being written straight
     * away: the trace has to come out in the order the receiver delivered, because that is the
     * order it will be replayed in.
     */
    private sealed interface Waiting {
        val reading: UserLocation

        data class Candidate(
            override val reading: UserLocation,
            val smoothed: UserLocation,
        ) : Waiting

        data class TurnedAway(
            override val reading: UserLocation,
            val reason: DiscardReason,
        ) : Waiting
    }

    fun accepting(location: UserLocation): LocationFilterResult {
        if (location.usableAccuracy() > MAX_USABLE_ACCURACY_METERS) {
            return turningAway(DiscardReason.POOR_ACCURACY, location)
        }

        val previous = trail.lastOrNull() ?: anchor
        if (previous != null && impliesImpossibleTravel(previous, location)) {
            // Deliberately not fed to the smoother: one absurd fix would drag the estimate with it.
            return turningAway(DiscardReason.IMPLAUSIBLE_SPEED, location)
        }

        val smoothing = smoother.smoothing(location)
        val smoothed = smoothing.location
        val candidate = Waiting.Candidate(location, smoothed)
        val remembered = trail.trailing(smoothed)
        val anchored = anchor ?: return releasing(
            smoother = smoothing.filter,
            trail = remembered,
            queue = listOf(candidate),
            path = listOf(candidate),
            departedAtMillis = null,
            travellingUntilMillis = null,
        )

        if (anchored.metersTo(smoothed) < noiseFloorMeters(location)) {
            // Back where the path already is, which cancels the departure outright: an excursion
            // that comes home was the signal wandering, however far out it got. This reading still
            // refines the estimate, and measuring the next departure from the last *released* point
            // is what lets slow movement add up over several readings instead of being discarded
            // one by one forever.
            return LocationFilterResult.Discarded(
                filter = LocationFilter(
                    smoother = smoothing.filter,
                    anchor = anchored,
                    trail = remembered,
                    waiting = emptyList(),
                    // The wait starts again only if the receiver has actually been *around* the
                    // anchor lately, not because one reading landed back inside the floor. At the
                    // three-metre floor of a clean signal a walker produces those constantly, and
                    // restarting on each of them left a real walk unable to earn its first window
                    // until several had gone by.
                    departedAtMillis = departedAtMillis.takeUnless {
                        remembered.hasBeenAround(anchored, location.timestamp)
                    },
                    travellingUntilMillis = travellingUntilMillis,
                ),
                reason = DiscardReason.WITHIN_NOISE,
                settled = waiting.settledAs(DiscardReason.UNCONFIRMED_MOVEMENT) +
                    TraceRecord(location, DiscardReason.WITHIN_NOISE),
            )
        }

        val queue = waiting + candidate
        val departedAt = departedAtMillis ?: location.timestamp

        if (travellingUntilMillis != null && location.timestamp <= travellingUntilMillis) {
            // Travel already established and still running: the path grows as the readings arrive,
            // which is what keeps the distance live instead of arriving one window at a time.
            return releasing(
                smoother = smoothing.filter,
                trail = remembered,
                queue = queue,
                path = queue.candidates(),
                departedAtMillis = departedAt,
                travellingUntilMillis = travellingUntilMillis,
            )
        }

        if (location.timestamp - departedAt < CONFIRMATION_WINDOW_MILLIS) {
            return LocationFilterResult.Discarded(
                filter = LocationFilter(
                    smoother = smoothing.filter,
                    anchor = anchored,
                    trail = remembered,
                    waiting = queue,
                    departedAtMillis = departedAt,
                    travellingUntilMillis = travellingUntilMillis,
                ),
                reason = DiscardReason.UNCONFIRMED_MOVEMENT,
                // Nothing is final yet: this reading may still turn out to be part of the path.
                settled = emptyList(),
            )
        }

        val progress = remembered.progressOverTheWindow(location.timestamp)
        if (progress != null && progress.wentSomewhere) {
            // A whole window away from the path, and the receiver has genuinely been getting
            // somewhere over it: this is travel, and what was held on the way belongs to it —
            // except anything older than the window itself. Those readings sat through a whole
            // window that never confirmed them, which is what happens when the receiver goes quiet
            // or starts reporting fixes too poor to use, and a position taken after that gap says
            // nothing about where it was before.
            val fresh = queue.filter { it.reading.timestamp >= location.timestamp - CONFIRMATION_WINDOW_MILLIS }
            return releasing(
                smoother = smoothing.filter,
                trail = remembered,
                queue = queue,
                path = fresh.candidates(),
                departedAtMillis = departedAt,
                travellingUntilMillis = location.timestamp + CONFIRMATION_WINDOW_MILLIS,
            )
        }

        // A window out that got nowhere: a wander, not a journey. The next is judged afresh.
        return LocationFilterResult.Discarded(
            filter = LocationFilter(
                smoother = smoothing.filter,
                anchor = anchored,
                trail = remembered,
                waiting = emptyList(),
                departedAtMillis = location.timestamp,
                travellingUntilMillis = null,
            ),
            reason = DiscardReason.UNCONFIRMED_MOVEMENT,
            settled = queue.settledAs(DiscardReason.UNCONFIRMED_MOVEMENT),
        )
    }

    /**
     * What becomes of a departure the recording will never get to confirm, because it is ending.
     *
     * Judged with what it has instead of the window it will not get: waiting is not an option
     * and the alternative is silently losing however much travel happened after the last
     * confirmation — which, for someone who presses stop while still walking, is the end of their
     * outing.
     */
    fun closing(): Closing {
        if (waiting.isEmpty()) return Closing(emptyList(), emptyList())

        val path = waiting.candidates()
        val judgedFrom = trail.firstOrNull() ?: anchor
        val wentSomewhere = path.isNotEmpty() && (
            judgedFrom == null ||
                judgedFrom.metersTo(path.last().smoothed) >= noiseFloorMeters(path.last().reading)
            )

        return if (wentSomewhere) {
            Closing(
                released = path.thinnedToTheNoiseFloor().map { it.smoothed },
                settled = waiting.settledAs(released = path.toSet()),
            )
        } else {
            Closing(emptyList(), waiting.settledAs(DiscardReason.UNCONFIRMED_MOVEMENT))
        }
    }

    /** Everything the filter was still holding when the recording stopped. */
    data class Closing(
        val released: List<UserLocation>,
        val settled: List<TraceRecord>,
    )

    /**
     * A reading turned away on its own merits. Its verdict is final, but it can only be written once
     * every older reading has one too.
     */
    private fun turningAway(reason: DiscardReason, location: UserLocation): LocationFilterResult {
        if (waiting.isEmpty()) {
            return LocationFilterResult.Discarded(this, reason, listOf(TraceRecord(location, reason)))
        }
        return LocationFilterResult.Discarded(
            filter = LocationFilter(
                smoother = smoother,
                anchor = anchor,
                trail = trail,
                waiting = waiting + Waiting.TurnedAway(location, reason),
                departedAtMillis = departedAtMillis,
                travellingUntilMillis = travellingUntilMillis,
            ),
            reason = reason,
            settled = emptyList(),
        )
    }

    private fun releasing(
        smoother: KalmanFilter,
        trail: List<UserLocation>,
        queue: List<Waiting>,
        path: List<Waiting.Candidate>,
        departedAtMillis: Long?,
        travellingUntilMillis: Long?,
    ): LocationFilterResult = LocationFilterResult.Accepted(
        filter = LocationFilter(
            smoother = smoother,
            anchor = path.last().smoothed,
            trail = trail,
            waiting = emptyList(),
            // The departure stays open: the path has caught up with it, and someone who never comes
            // back to the anchor should not have to earn another window of patience.
            departedAtMillis = departedAtMillis,
            travellingUntilMillis = travellingUntilMillis,
        ),
        locations = path.thinnedToTheNoiseFloor().map { it.smoothed },
        settled = queue.settledAs(released = path.toSet()),
    )

    private fun List<Waiting>.candidates(): List<Waiting.Candidate> =
        filterIsInstance<Waiting.Candidate>()

    private fun List<Waiting>.settledAs(fallback: DiscardReason): List<TraceRecord> =
        map { TraceRecord(it.reading, (it as? Waiting.TurnedAway)?.reason ?: fallback) }

    private fun List<Waiting>.settledAs(released: Set<Waiting.Candidate>): List<TraceRecord> =
        map { entry ->
            TraceRecord(
                reading = entry.reading,
                discardReason = when {
                    entry in released -> null
                    entry is Waiting.TurnedAway -> entry.reason
                    else -> DiscardReason.UNCONFIRMED_MOVEMENT
                },
            )
        }

    /**
     * Keeps the readings of a confirmed departure that are far enough apart to be telling the path
     * something, and drops the ones that only report where it already was.
     *
     * The noise floor is the resolution of the path: a reading closer to the previous point than its
     * own uncertainty says nothing about the shape and lengthens the route by its own error. Holding
     * a departure must not change that resolution — measured on a walk under a coarse signal,
     * releasing every held reading inflated the recorded distance by 14 %.
     *
     * The last one always survives: it is where the user actually is.
     */
    private fun List<Waiting.Candidate>.thinnedToTheNoiseFloor(): List<Waiting.Candidate> {
        if (size <= 1) return this
        val kept = mutableListOf<Waiting.Candidate>()
        var previous: UserLocation? = null
        forEach { candidate ->
            val from = previous
            if (from == null || from.metersTo(candidate.smoothed) >= noiseFloorMeters(candidate.reading)) {
                kept += candidate
                previous = candidate.smoothed
            }
        }
        val last = last()
        if (kept.lastOrNull() !== last) kept += last
        return kept
    }

    /**
     * The trail with this position on the end, cut to the last window plus the one entry just
     * before it — that older entry is what says where the receiver was when the window opened, so
     * dropping it would throw away the comparison the filter exists to make.
     */
    private fun List<UserLocation>.trailing(next: UserLocation): List<UserLocation> {
        val extended = this + next
        val oldestNeeded = extended.indexOfLast {
            next.timestamp - it.timestamp >= CONFIRMATION_WINDOW_MILLIS
        }
        return if (oldestNeeded <= 0) extended else extended.drop(oldestNeeded)
    }

    /**
     * How far the receiver has actually got over the window: where it has been during the recent
     * half against where it was during the half before that. Null until the trail covers a whole
     * window, because before that there is nothing to compare.
     */
    private fun List<UserLocation>.progressOverTheWindow(nowMillis: Long): Progress? {
        if (isEmpty() || first().timestamp > nowMillis - CONFIRMATION_WINDOW_MILLIS) return null

        val turn = nowMillis - CONFIRMATION_WINDOW_MILLIS / 2
        val before = filter { it.timestamp <= turn }
        val since = filter { it.timestamp > turn }
        if (before.isEmpty() || since.isEmpty()) return null

        val from = before.middle()
        val to = since.middle()
        val seconds = (to.atMillis - from.atMillis) / MILLIS_PER_SECOND
        if (seconds <= 0.0) return null
        return Progress(
            meters = haversineMeters(from.latitude, from.longitude, to.latitude, to.longitude),
            seconds = seconds,
            // The uncertainty of the window, not of whichever reading happens to be asking. A
            // comparison between two robust positions deserves a robust threshold: the parked half
            // hour kept producing single fixes claiming three metres, and judging fifteen metres of
            // median-to-median wander against three of them called it a journey.
            noiseFloorMeters = map { noiseFloorMeters(it) }.sorted()[size / 2],
        )
    }

    /**
     * Whether the receiver has spent the recent half of the window around [place], judged by where
     * it has been rather than by where this one reading puts it.
     */
    private fun List<UserLocation>.hasBeenAround(place: UserLocation, nowMillis: Long): Boolean {
        val recent = filter { it.timestamp > nowMillis - CONFIRMATION_WINDOW_MILLIS / 2 }
        if (recent.isEmpty()) return true
        val middle = recent.middle()
        val floor = recent.map { noiseFloorMeters(it) }.sorted()[recent.size / 2]
        return haversineMeters(place.latitude, place.longitude, middle.latitude, middle.longitude) < floor
    }

    /** How far the receiver got over the window, over how long, and how sure the window is of it. */
    private data class Progress(
        val meters: Double,
        val seconds: Double,
        val noiseFloorMeters: Double,
    ) {
        val metersPerSecond: Double get() = meters / seconds
        val wentSomewhere: Boolean get() = meters >= noiseFloorMeters
    }

    /**
     * The median latitude, longitude and time of these readings: a place the receiver has actually
     * been near, rather than an average that a single outlier can pull off the map.
     */
    private fun List<UserLocation>.middle(): Middle = Middle(
        latitude = map { it.latitude }.sorted()[size / 2],
        longitude = map { it.longitude }.sorted()[size / 2],
        atMillis = map { it.timestamp }.sorted()[size / 2],
    )

    private data class Middle(val latitude: Double, val longitude: Double, val atMillis: Long)

    private fun UserLocation.metersTo(other: UserLocation): Double =
        haversineMeters(latitude, longitude, other.latitude, other.longitude)

    /**
     * Whether this reading claims something no one could have done since the previous one.
     *
     * Two bounds, because the field trace showed that one is not enough. The fixed one catches the
     * receiver teleporting outright. The second catches what it does not: a phone parked on a table
     * reported itself 168 m away nine seconds later, sat there claiming ±3 m of accuracy, and came
     * back — a wrong fix, confidently held, and indistinguishable from travel by position alone.
     * What gives it away is that **what the receiver has been doing lately bounds what it can
     * plausibly do next**, which also keeps the rule activity-neutral without being told the
     * activity: it is read off the trace instead of assumed.
     *
     * Judged as a distance rather than as a speed, because the two differ exactly where the wrong
     * fixes live: a receiver that goes quiet for nine seconds and comes back 168 m away passes any
     * speed limit that multiplies by the gap, while nobody covers 168 m from a standstill in nine
     * seconds however hard they accelerate.
     */
    private fun impliesImpossibleTravel(previous: UserLocation, location: UserLocation): Boolean {
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
        val uncertainty = location.usableAccuracy()
        if ((apparent - uncertainty).coerceAtLeast(0.0) / elapsedSeconds > MAX_PLAUSIBLE_SPEED_MPS) {
            return true
        }

        val progress = trail.progressOverTheWindow(location.timestamp) ?: return false
        val startingSpeed = maxOf(progress.metersPerSecond, MIN_UNSURPRISING_SPEED_MPS)
        val reachable = startingSpeed * elapsedSeconds +
            MAX_PLAUSIBLE_ACCELERATION_MPS2 * elapsedSeconds * elapsedSeconds / 2
        // Both ends of the comparison carry error, and neither is where the receiver truly was.
        return apparent > reachable + 2 * uncertainty
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

        /**
         * About what a cyclist manages out of a standstill — nought to 25 km/h in seven seconds —
         * and well under what a car does. Anything faster than this, out of what the receiver has
         * been doing, is the fix jumping rather than the user moving.
         */
        const val MAX_PLAUSIBLE_ACCELERATION_MPS2 = 1.0

        /**
         * Below this there is nothing to be suspicious about, whatever the receiver was doing
         * before: someone who begins recording already moving must not have their first readings
         * judged against a standstill.
         */
        const val MIN_UNSURPRISING_SPEED_MPS = 2.0

        /** Beyond this a fix says little more than "somewhere around here". */
        const val MAX_USABLE_ACCURACY_METERS = 50.0

        const val MIN_NOISE_FLOOR_METERS = 3.0
        const val MAX_NOISE_FLOOR_METERS = 20.0

        /**
         * How far back the filter looks to tell travel from a signal going in circles, and so also
         * how long a recording waits before its first metre appears on screen.
         *
         * **A minute is what the field traces support, and the wait was measured rather than
         * assumed.** Replaying the four of them through this filter, the phone parked for half an
         * hour records 165 m of wander over a minute-long window, 1433 m over twenty seconds and
         * 1613 m over thirty, while the real walk and the real ride keep their distance at every
         * value. Half the wait costs an order of magnitude of the lie, so the wait stays.
         *
         * It is a duration and not a distance on purpose: a minute asks a cyclist and a walker for
         * the same patience, while any number of metres would be a different demand for each.
         */
        const val CONFIRMATION_WINDOW_MILLIS = 60_000L

        const val MILLIS_PER_SECOND = 1_000.0
    }
}
