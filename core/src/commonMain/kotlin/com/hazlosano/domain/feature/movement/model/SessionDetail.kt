package com.hazlosano.domain.feature.movement.model

/**
 * A recorded session together with the route it stored and the figures that route shows.
 *
 * [stats] is computed from [path] when the session is read, which is what makes improving the
 * measurement improve the sessions already recorded.
 */
data class SessionDetail(
    val session: MovementSession,
    val path: List<UserLocation>,
    val stats: SessionStats = SessionStats(),
    /** Distance measured from [path] now, which may differ from the summary stored with the session. */
    val distanceMeters: Double = 0.0,
) {
    /** A path needs at least two points to be drawn as a route. */
    val hasPath: Boolean
        get() = path.size >= 2
}
