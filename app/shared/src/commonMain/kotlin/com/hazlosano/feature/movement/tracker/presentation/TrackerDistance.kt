package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.RecordingState

/**
 * What the tracker can honestly say about the distance of a recording in progress.
 *
 * A recording has no distance from its first second. A departure from the path only becomes metres
 * once it has held up for a minute — without that wait a phone on a table records kilometres — so
 * during that minute there is nothing measured to show. Showing `0,00 km` there is the app looking
 * like it has measured and found nothing, which is a different claim from "not yet", and the one a
 * user reads as a broken recording.
 *
 * A type rather than a worded string: the wording belongs in the resource catalogue, and a test
 * that asserts on it would be asserting on the copy.
 */
sealed interface TrackerDistance {

    /** No fix has arrived yet, so there is not even a starting point. */
    data object WaitingForAFix : TrackerDistance

    /** The path has begun and the receiver has yet to show it is going anywhere. */
    data object Confirming : TrackerDistance

    /** A measured distance, which is the case for every recording past its first minute. */
    data class Travelled(val meters: Double) : TrackerDistance
}

fun RecordingState.trackerDistance(): TrackerDistance = when {
    // Idle shows the zero it has always shown: nothing is pending, there is simply no session.
    !isRecording -> TrackerDistance.Travelled(distanceMeters)
    traveledPoints.isEmpty() -> TrackerDistance.WaitingForAFix
    // One point is where the recording started, not a journey: the filter is still holding whatever
    // has arrived since, and will release it as soon as the departure holds up.
    traveledPoints.size == 1 -> TrackerDistance.Confirming
    else -> TrackerDistance.Travelled(distanceMeters)
}
