package com.hazlosano.feature.movement.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hazlosano.domain.feature.movement.model.UserLocation

/**
 * Platform-specific map surface for the movement pillar, shared by the live tracker and the detail
 * of a recorded session. Android renders a MapLibre map that shows [userLocation] as the current
 * position and draws [path] as a route; other targets render a placeholder.
 *
 * Set [fitPathInView] to frame the whole [path] instead of following the live position — that is
 * what a finished session needs, since it has no current position to follow.
 */
@Composable
expect fun MovementMap(
    userLocation: UserLocation?,
    path: List<UserLocation>,
    modifier: Modifier = Modifier,
    fitPathInView: Boolean = false,
)
