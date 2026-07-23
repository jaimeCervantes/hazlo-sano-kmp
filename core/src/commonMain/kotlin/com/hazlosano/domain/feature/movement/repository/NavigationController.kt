package com.hazlosano.domain.feature.movement.repository

import com.hazlosano.domain.feature.movement.model.NavigationState
import com.hazlosano.domain.feature.movement.model.Route
import kotlinx.coroutines.flow.StateFlow

interface NavigationController {
    val isNavigating: StateFlow<Boolean>
    val navigationState: StateFlow<NavigationState>

    fun startNavigation(route: Route?)
    fun stopNavigation()
}
