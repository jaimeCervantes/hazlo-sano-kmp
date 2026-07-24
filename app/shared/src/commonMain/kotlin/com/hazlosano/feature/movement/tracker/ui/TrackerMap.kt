package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hazlosano.domain.feature.movement.model.UserLocation

/**
 * Platform-specific map surface for the movement tracker. Android renders a MapLibre map, shows
 * [userLocation] as the current position and draws [traveledPoints] as the recorded path; other
 * targets render a placeholder.
 */
@Composable
expect fun TrackerMap(
    userLocation: UserLocation?,
    traveledPoints: List<UserLocation>,
    modifier: Modifier = Modifier,
)
