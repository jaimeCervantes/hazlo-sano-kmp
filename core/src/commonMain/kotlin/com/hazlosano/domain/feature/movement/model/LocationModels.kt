package com.hazlosano.domain.feature.movement.model

/**
 * One reading from the receiver.
 *
 * [altitude] and [verticalAccuracy] are null when the receiver did not report them, never zero. A
 * fix without an altitude is not a position at sea level, and Android's `getAltitude()` returns 0.0
 * exactly when `hasAltitude()` is false — reading it without checking is how a missing altitude
 * becomes a confident claim about the terrain.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float = 0f,
    /** What the receiver says about its own altitude; null when it says nothing. */
    val verticalAccuracy: Float? = null,
    val bearing: Float = 0f,
    val timestamp: Long = 0L,
)
