package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar

/**
 * Movement tracker screen: a top bar with a back control over the platform map surface.
 */
@Composable
fun TrackerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(
            title = "Movimiento",
            showBackButton = true,
            onBackClick = onBack,
        )
        TrackerMap(modifier = Modifier.weight(1f).fillMaxSize())
    }
}
