package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hazlosano.domain.feature.movement.model.UserLocation

@Composable
actual fun TrackerMap(
    userLocation: UserLocation?,
    traveledPoints: List<UserLocation>,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "El mapa está disponible en Android por ahora.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
