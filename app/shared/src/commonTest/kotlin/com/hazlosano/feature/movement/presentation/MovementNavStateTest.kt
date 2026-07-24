package com.hazlosano.feature.movement.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MovementNavStateTest {

    @Test
    fun startsClosed() {
        val state = MovementNavState()

        assertFalse(state.isOpen)
        assertEquals(MovementDestination.Closed, state.destination)
    }

    @Test
    fun opensTheTrackerWhenMovementCardTapped() {
        val state = MovementNavState()

        state.openTracker()

        assertTrue(state.isOpen)
        assertEquals(MovementDestination.Tracker, state.destination)
    }

    @Test
    fun opensTheHistoryFromTheTracker() {
        val state = MovementNavState().apply { openTracker() }

        state.openHistory()

        assertEquals(MovementDestination.History, state.destination)
    }

    @Test
    fun returnsToTheTrackerFromTheHistory() {
        val state = MovementNavState().apply { openHistory() }

        state.openTracker()

        assertEquals(MovementDestination.Tracker, state.destination)
    }

    @Test
    fun closesWhenBackControlTapped() {
        val state = MovementNavState().apply { openTracker() }

        state.close()

        assertFalse(state.isOpen)
        assertEquals(MovementDestination.Closed, state.destination)
    }
}
