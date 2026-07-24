package com.hazlosano.feature.movement.tracker.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Holds the visibility of the movement tracker screen so the navigation decision lives outside the
 * Composable and can be unit-tested. Backed by Compose snapshot state to drive recomposition.
 */
class TrackerNavState {
    var isOpen: Boolean by mutableStateOf(false)
        private set

    fun open() {
        isOpen = true
    }

    fun close() {
        isOpen = false
    }
}
