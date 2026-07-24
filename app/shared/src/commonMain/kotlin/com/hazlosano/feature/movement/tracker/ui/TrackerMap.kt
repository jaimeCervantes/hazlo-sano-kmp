package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific map surface for the movement tracker. Android renders a MapLibre map; other
 * targets render a placeholder until their map integration exists.
 */
@Composable
expect fun TrackerMap(modifier: Modifier = Modifier)
