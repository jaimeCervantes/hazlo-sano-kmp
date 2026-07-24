package com.hazlosano.feature.movement.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class MovementDestination { Closed, Tracker, History }

/**
 * Navigation inside the movement pillar (tracker and its session history) so the decision lives
 * outside the Composables and can be unit-tested. Backed by Compose snapshot state to drive
 * recomposition.
 */
class MovementNavState {
    var destination: MovementDestination by mutableStateOf(MovementDestination.Closed)
        private set

    val isOpen: Boolean
        get() = destination != MovementDestination.Closed

    fun openTracker() {
        destination = MovementDestination.Tracker
    }

    fun openHistory() {
        destination = MovementDestination.History
    }

    fun close() {
        destination = MovementDestination.Closed
    }
}
