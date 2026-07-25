package com.hazlosano.domain.feature.movement.model

import com.hazlosano.domain.geo.haversineMeters

/**
 * What has been recorded of the session in progress.
 *
 * Elapsed time is derived from [startedAtMillis] rather than counted by a timer, so a recording that
 * runs with the screen off or the app in the background still reports the real duration instead of
 * however many ticks the system allowed.
 */
data class RecordingState(
    val isRecording: Boolean = false,
    val startedAtMillis: Long? = null,
    val traveledPoints: List<UserLocation> = emptyList(),
    val distanceMeters: Double = 0.0,
    val elapsedSeconds: Long = 0,
)

/** Begins a new recording, discarding anything recorded before. */
fun RecordingState.started(nowMillis: Long): RecordingState =
    RecordingState(isRecording = true, startedAtMillis = nowMillis)

/** Adds a location to the traveled path. Locations arriving while idle are ignored. */
fun RecordingState.recorded(location: UserLocation): RecordingState {
    if (!isRecording) return this

    val previous = traveledPoints.lastOrNull()
    val addedMeters = previous?.let {
        haversineMeters(it.latitude, it.longitude, location.latitude, location.longitude)
    } ?: 0.0

    return copy(
        traveledPoints = traveledPoints + location,
        distanceMeters = distanceMeters + addedMeters,
    )
}

/** Refreshes the elapsed time of a recording in progress. */
fun RecordingState.elapsedAt(nowMillis: Long): RecordingState {
    val startedAt = startedAtMillis
    if (!isRecording || startedAt == null) return this
    return copy(elapsedSeconds = ((nowMillis - startedAt) / MILLIS_PER_SECOND).coerceAtLeast(0))
}

/** Ends the recording, keeping what was recorded so it can be saved. */
fun RecordingState.stopped(nowMillis: Long): RecordingState =
    elapsedAt(nowMillis).copy(isRecording = false)

private const val MILLIS_PER_SECOND = 1_000L
