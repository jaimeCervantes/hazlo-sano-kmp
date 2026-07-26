package com.hazlosano.domain.feature.movement.model

/**
 * What the recorded route of a finished session shows.
 *
 * Derived from the stored points every time a session is read rather than written down when it
 * stopped. A figure written down is frozen at whatever version of the algorithm produced it, so
 * improving how the app measures would leave every session already recorded reporting the old
 * number forever — and adding a new figure would cost a schema change.
 *
 * Every field is nullable and null means **not measured**, never zero. A session whose readings
 * carried no altitude has no climb rather than a climb of zero: reporting zero would claim flat
 * ground, which is an assertion the recording never made.
 */
data class SessionStats(
    val movingTime: Long? = null,
    val avgPace: Double? = null, // min/km
    val maxAltitude: Double? = null,
    val minAltitude: Double? = null,
    val totalAscent: Double? = null,
    val totalDescent: Double? = null,
    val avgSlope: Double? = null, // %
    val maxSlope: Double? = null, // %
    val vam: Double? = null, // vertical ascent velocity, m/h
)
