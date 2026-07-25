package com.hazlosano.domain.feature.movement.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingStateTest {

    @Test
    fun `starts empty and recording`() {
        val state = RecordingState().started(nowMillis = START)

        assertTrue(state.isRecording)
        assertEquals(START, state.startedAtMillis)
        assertEquals(emptyList(), state.traveledPoints)
        assertEquals(0.0, state.distanceMeters)
        assertEquals(0, state.elapsedSeconds)
    }

    @Test
    fun `ignores locations while idle`() {
        val state = RecordingState().recorded(location(19.4300, -99.1300))

        assertEquals(emptyList(), state.traveledPoints)
        assertEquals(0.0, state.distanceMeters)
    }

    @Test
    fun `accumulates the traveled path and its distance`() {
        val state = RecordingState()
            .started(START)
            .recorded(location(19.4300, -99.1300))
            .recorded(location(19.4310, -99.1300))
            .recorded(location(19.4320, -99.1300))

        assertEquals(3, state.traveledPoints.size)
        // Three points ~111 m apart in latitude: roughly 222 m in total.
        assertTrue(state.distanceMeters in 200.0..250.0, "distance was ${state.distanceMeters}")
    }

    @Test
    fun `derives elapsed time from wall clock instead of counting ticks`() {
        val state = RecordingState()
            .started(START)
            .elapsedAt(START + 90_000)

        assertEquals(90, state.elapsedSeconds)
    }

    @Test
    fun `keeps the whole elapsed time even when nothing refreshed it in between`() {
        // The screen was off for ten minutes: no tick arrived, but the session did last that long.
        val state = RecordingState()
            .started(START)
            .elapsedAt(START + 1_000)
            .elapsedAt(START + 600_000)

        assertEquals(600, state.elapsedSeconds)
    }

    @Test
    fun `stopping keeps what was recorded and freezes the elapsed time`() {
        val state = RecordingState()
            .started(START)
            .recorded(location(19.4300, -99.1300))
            .recorded(location(19.4310, -99.1300))
            .stopped(START + 300_000)

        assertFalse(state.isRecording)
        assertEquals(2, state.traveledPoints.size)
        assertEquals(300, state.elapsedSeconds)
        assertEquals(300, state.elapsedAt(START + 900_000).elapsedSeconds)
    }

    @Test
    fun `does not report elapsed time before starting`() {
        assertEquals(0, RecordingState().elapsedAt(START).elapsedSeconds)
    }

    private fun location(latitude: Double, longitude: Double): UserLocation =
        UserLocation(latitude = latitude, longitude = longitude)
}

private const val START = 1_784_877_300_000L
