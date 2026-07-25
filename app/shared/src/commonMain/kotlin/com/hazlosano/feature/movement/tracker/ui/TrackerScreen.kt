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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.hazlosano.feature.movement.presentation.MovementFormat
import com.hazlosano.feature.movement.tracker.presentation.createTrackerViewModel
import com.hazlosano.feature.movement.ui.MovementMap
import kotlinx.coroutines.delay

/** How long the "session saved" confirmation stays on screen after a recording ends. */
private const val SAVED_CONFIRMATION_MILLIS = 5_000L

/**
 * Movement tracker screen: a map showing the user's live location, live session metrics, and a
 * start/stop control that records the traveled path.
 */
@Composable
fun TrackerScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { createTrackerViewModel() }
    LocationPermissionEffect(onGranted = viewModel::startTracking)

    val userLocation by viewModel.userLocation.collectAsState()
    val recording by viewModel.recording.collectAsState()
    val savedSession by viewModel.lastSavedSession.collectAsState()
    val isRecording = recording.isRecording

    // The confirmation is transient: without this it would greet the user again every time they
    // came back to the tracker, long after the session was saved.
    LaunchedEffect(savedSession) {
        if (savedSession != null) {
            delay(SAVED_CONFIRMATION_MILLIS)
            viewModel.acknowledgeSavedSession()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(title = "Movimiento", showBackButton = true, onBackClick = onBack)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            MovementMap(
                userLocation = userLocation,
                path = recording.traveledPoints,
                modifier = Modifier.fillMaxSize(),
            )
            SessionMetrics(
                distanceMeters = recording.distanceMeters,
                elapsedSeconds = recording.elapsedSeconds,
                modifier = Modifier.align(Alignment.TopStart).padding(HazloSpaces.gutter),
            )
        }

        savedSession?.let { saved ->
            Text(
                text = "Sesión guardada · ${MovementFormat.distance(saved.distanceMeters)} · " +
                    MovementFormat.duration(saved.elapsedSeconds),
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
            onOpenHistory = onOpenHistory,
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
            Metric(label = "Distancia", value = MovementFormat.distance(distanceMeters))
            Metric(label = "Tiempo", value = MovementFormat.duration(elapsedSeconds))
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
    onOpenHistory: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(HazloSpaces.gutter),
        horizontalArrangement = Arrangement.spacedBy(HazloSpaces.md, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
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
        OutlinedButton(onClick = onOpenHistory) {
            Text("Historial")
        }
    }
}
