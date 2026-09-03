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
import androidx.compose.material3.Switch
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
import com.hazlosano.domain.feature.movement.model.goneNowhereMinutes
import com.hazlosano.feature.movement.presentation.MovementFormat
import com.hazlosano.feature.movement.tracker.presentation.TrackerDistance
import com.hazlosano.feature.movement.tracker.presentation.createTrackerViewModel
import com.hazlosano.feature.movement.tracker.presentation.trackerDistance
import com.hazlosano.feature.movement.ui.MovementMap
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.tracker_distance_confirming
import hazlosano.app.shared.generated.resources.tracker_distance_waiting_for_fix
import hazlosano.app.shared.generated.resources.tracker_gone_nowhere
import hazlosano.app.shared.generated.resources.top_app_bar_back
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

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
    val captureTrace by viewModel.captureTrace.collectAsState()
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
        HazloTopAppBar(
            title = "Movimiento",
            showBackButton = true,
            onBackClick = onBack,
            backContentDescription = stringResource(Res.string.top_app_bar_back),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            MovementMap(
                userLocation = userLocation,
                path = recording.traveledPoints,
                modifier = Modifier.fillMaxSize(),
            )
            SessionMetrics(
                distance = recording.trackerDistance(),
                elapsedSeconds = recording.elapsedSeconds,
                modifier = Modifier.align(Alignment.TopStart).padding(HazloSpaces.gutter),
            )
            recording.goneNowhereMinutes()?.let { minutes ->
                GoneNowhereNotice(
                    minutes = minutes,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(HazloSpaces.gutter),
                )
            }
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
        // Only offered while idle: the choice applies to the recording being started, and showing a
        // switch that silently does nothing mid-session would be a lie.
        if (!isRecording) {
            TraceCaptureToggle(enabled = captureTrace, onChange = viewModel::setCaptureTrace)
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
private fun TraceCaptureToggle(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HazloSpaces.gutter),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Guardar traza de diagnóstico",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Guarda cada lectura del GPS para ajustar el filtro.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

/**
 * Said out loud because the recording that produced this rule ran for half an hour on a table while
 * its owner had moved on to something else. The app cannot know whether that is a mistake, so it
 * reports what it sees and leaves the stop button where it was.
 */
@Composable
private fun GoneNowhereNotice(minutes: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
        tonalElevation = 3.dp,
    ) {
        Text(
            text = stringResource(Res.string.tracker_gone_nowhere, minutes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = HazloSpaces.md, vertical = HazloSpaces.sm),
        )
    }
}

@Composable
private fun SessionMetrics(
    distance: TrackerDistance,
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
            Metric(label = "Distancia", value = distance.shown())
            Metric(label = "Tiempo", value = MovementFormat.duration(elapsedSeconds))
        }
    }
}

/**
 * A recording that has not confirmed any movement yet says so, rather than showing the zero it
 * would show for a phone that had genuinely gone nowhere. The two look identical on screen and mean
 * opposite things, and the zero is the one that reads as a broken recording.
 */
@Composable
private fun TrackerDistance.shown(): String = when (this) {
    TrackerDistance.WaitingForAFix -> stringResource(Res.string.tracker_distance_waiting_for_fix)
    TrackerDistance.Confirming -> stringResource(Res.string.tracker_distance_confirming)
    is TrackerDistance.Travelled -> MovementFormat.distance(meters)
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
