package com.hazlosano.domain.feature.movement.model

/** A recorded session together with the path it stored, as the detail view needs it. */
data class SessionDetail(
    val session: MovementSession,
    val path: List<UserLocation>,
) {
    /** A path needs at least two points to be drawn as a route. */
    val hasPath: Boolean
        get() = path.size >= 2
}
