package com.hazlosano.domain.feature.movement.filter

import com.hazlosano.domain.feature.movement.model.UserLocation

/**
 * One reading exactly as the receiver delivered it, together with what [LocationFilter] decided
 * about it.
 *
 * A recorded session only keeps the readings that were accepted, already smoothed, so nothing
 * survives of what was thrown away or why. That is enough to see that a distance came out wrong and
 * not enough to know why, which is what makes the thresholds impossible to settle without walking
 * the route again. This is the missing half.
 *
 * The reading is kept **raw** on purpose: the smoothed position can always be recomputed by feeding
 * these back through the filter, which is precisely what makes a captured trace replayable against
 * a different set of thresholds.
 */
data class TraceRecord(
    val reading: UserLocation,
    /** Why the filter rejected this reading, or null when it became part of the path. */
    val discardReason: DiscardReason?,
) {
    val wasAccepted: Boolean
        get() = discardReason == null
}
