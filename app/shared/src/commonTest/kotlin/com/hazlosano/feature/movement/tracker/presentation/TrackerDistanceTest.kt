package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the `@slice-2` scenario of `features/movement_drifting_signal.feature`: the tracker says
 * what it is waiting for instead of showing a zero as if nothing had been walked.
 */
class TrackerDistanceTest {

    @Test
    fun aRecordingWithoutAFixYetIsWaitingForOne() {
        val state = RecordingState(isRecording = true)

        assertEquals(TrackerDistance.WaitingForAFix, state.trackerDistance())
    }

    @Test
    fun aRecordingWithOnlyItsStartingPointIsStillConfirming() {
        val state = RecordingState(isRecording = true, traveledPoints = listOf(readingAt(0L)))

        assertEquals(TrackerDistance.Confirming, state.trackerDistance())
    }

    @Test
    fun aRecordingThatHasGotSomewhereReportsTheDistance() {
        val state = RecordingState(
            isRecording = true,
            traveledPoints = listOf(readingAt(0L), readingAt(60_000L)),
            distanceMeters = 84.0,
        )

        assertEquals(TrackerDistance.Travelled(84.0), state.trackerDistance())
    }

    @Test
    fun anIdleTrackerReportsTheZeroItAlwaysHas() {
        assertEquals(TrackerDistance.Travelled(0.0), RecordingState().trackerDistance())
    }

    private fun readingAt(atMillis: Long): UserLocation =
        UserLocation(latitude = 19.4300, longitude = -99.1300, accuracy = 8f, timestamp = atMillis)
}
