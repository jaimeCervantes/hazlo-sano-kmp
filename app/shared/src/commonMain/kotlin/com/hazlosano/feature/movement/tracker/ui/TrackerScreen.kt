package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.feature.movement.tracker.presentation.createTrackerViewModel
import kotlin.math.roundToInt

/**
 * Movement tracker screen: a map showing the user's live location, live session metrics, and a
 * start/stop control that records the traveled path.
 */
@Composable
fun TrackerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = remember { createTrackerViewModel() }
    LocationPermissionEffect(onGranted = viewModel::startTracking)

    val userLocation by viewModel.userLocation.collectAsState()
    val traveledPoints by viewModel.traveledPoints.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val distanceMeters by viewModel.distanceMeters.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val sessionSaved by viewModel.sessionSaved.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(title = "Movimiento", showBackButton = true, onBackClick = onBack)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            TrackerMap(
                userLocation = userLocation,
                traveledPoints = traveledPoints,
                modifier = Modifier.fillMaxSize(),
            )
            SessionMetrics(
                distanceMeters = distanceMeters,
                elapsedSeconds = elapsedSeconds,
                modifier = Modifier.align(Alignment.TopStart).padding(HazloSpaces.gutter),
            )
        }

        if (sessionSaved && !isRecording) {
            Text(
                text = "Sesión guardada",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
                textAlign = TextAlign.Center,
            )
        }
        SessionControls(
            isRecording = isRecording,
            onStart = viewModel::startRecording,
            onStop = viewModel::stopRecording,
        )
    }
}

@Composable
private fun SessionMetrics(
    distanceMeters: Double,
    elapsedSeconds: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = HazloSpaces.md, vertical = HazloSpaces.sm),
            horizontalArrangement = Arrangement.spacedBy(HazloSpaces.lg),
        ) {
            Metric(label = "Distancia", value = formatDistance(distanceMeters))
            Metric(label = "Tiempo", value = formatDuration(elapsedSeconds))
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionControls(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(HazloSpaces.gutter),
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = if (isRecording) onStop else onStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ),
        ) {
            Text(if (isRecording) "Detener" else "Iniciar")
        }
    }
}

private fun formatDistance(meters: Double): String =
    if (meters < 1000.0) {
        "${meters.roundToInt()} m"
    } else {
        val km = (meters / 1000.0 * 100).roundToInt() / 100.0
        "$km km"
    }

private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val mm = if (minutes < 10) "0$minutes" else "$minutes"
    val ss = if (seconds < 10) "0$seconds" else "$seconds"
    return "$mm:$ss"
}
