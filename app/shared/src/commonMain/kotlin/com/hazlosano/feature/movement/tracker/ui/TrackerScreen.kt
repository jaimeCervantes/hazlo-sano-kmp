package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.data.movement.createLocationRepository
import com.hazlosano.feature.movement.tracker.presentation.TrackerViewModel

/**
 * Movement tracker screen: a top bar with a back control over the platform map surface, showing the
 * user's live location once the location permission is granted.
 */
@Composable
fun TrackerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = remember { TrackerViewModel(createLocationRepository()) }
    LocationPermissionEffect(onGranted = viewModel::startTracking)
    val userLocation by viewModel.userLocation.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(
            title = "Movimiento",
            showBackButton = true,
            onBackClick = onBack,
        )
        TrackerMap(
            userLocation = userLocation,
            modifier = Modifier.weight(1f).fillMaxSize(),
        )
    }
}
