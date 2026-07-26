package com.hazlosano.domain.feature.movement.model

/**
 * A recorded session as the history browses it.
 *
 * Carries only what listing sessions needs. Everything the route shows — climb, descent, altitudes,
 * time spent moving — is derived from the stored points when a session is opened, so it lives on
 * [SessionDetail] instead of here: the history would otherwise have to load every point of every
 * session to draw a list.
 *
 * [distanceTraveled] is the exception, and deliberately so. It is derivable, but the list needs it
 * without reading any points, so it is kept as a summary written when the session was recorded and
 * refreshed when the session is opened.
 */
data class MovementSession(
    val id: Long = 0,
    val routeId: Long?,
    val name: String,
    val date: Long,
    val elapsedTime: Long, // in seconds
    val distanceTraveled: Double, // in meters
    val previewPoints: List<UserLocation>,
)
