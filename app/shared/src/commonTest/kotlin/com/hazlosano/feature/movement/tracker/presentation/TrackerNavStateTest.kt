package com.hazlosano.feature.movement.tracker.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackerNavStateTest {

    @Test
    fun startsClosed() {
        val state = TrackerNavState()

        assertFalse(state.isOpen)
    }

    @Test
    fun opensWhenMovementCardTapped() {
        val state = TrackerNavState()

        state.open()

        assertTrue(state.isOpen)
    }

    @Test
    fun closesWhenBackControlTapped() {
        val state = TrackerNavState().apply { open() }

        state.close()

        assertFalse(state.isOpen)
    }
}
