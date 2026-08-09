package com.hazlosano.feature.movement.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Where the movement pillar currently is. A sealed hierarchy (instead of an enum) so a destination
 * can carry what it needs — the session detail needs the session it shows.
 */
sealed interface MovementDestination {
    data object Closed : MovementDestination
    data object Tracker : MovementDestination
    data object History : MovementDestination
    data object Routes : MovementDestination
    data class SessionDetail(val sessionId: Long) : MovementDestination
}

/**
 * Navigation inside the movement pillar (tracker, session history and session detail) so the
 * decision lives outside the Composables and can be unit-tested. Backed by Compose snapshot state
 * to drive recomposition.
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

    fun openRoutes() {
        destination = MovementDestination.Routes
    }

    fun openSessionDetail(sessionId: Long) {
        destination = MovementDestination.SessionDetail(sessionId)
    }

    fun close() {
        destination = MovementDestination.Closed
    }
}
