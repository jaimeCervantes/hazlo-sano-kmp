package com.hazlosano.feature.movement.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.feature.movement.tracker.presentation.FollowedRoute
import hazlosano.app.shared.generated.resources.action_cancel
import hazlosano.app.shared.generated.resources.tracker_follow_route
import hazlosano.app.shared.generated.resources.tracker_following_route
import hazlosano.app.shared.generated.resources.tracker_pick_route_empty
import hazlosano.app.shared.generated.resources.tracker_pick_route_title
import hazlosano.app.shared.generated.resources.tracker_stop_following_route
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
import hazlosano.app.shared.generated.resources.movement_metric_distance
import hazlosano.app.shared.generated.resources.movement_metric_duration
import hazlosano.app.shared.generated.resources.tracker_open_history
import hazlosano.app.shared.generated.resources.tracker_session_saved
import hazlosano.app.shared.generated.resources.tracker_start
import hazlosano.app.shared.generated.resources.tracker_stop
import hazlosano.app.shared.generated.resources.tracker_title
import hazlosano.app.shared.generated.resources.tracker_trace_toggle_description
import hazlosano.app.shared.generated.resources.tracker_trace_toggle_title
import hazlosano.app.shared.generated.resources.tracker_distance_confirming
import hazlosano.app.shared.generated.resources.tracker_distance_waiting_for_fix
import hazlosano.app.shared.generated.resources.tracker_gone_nowhere
import hazlosano.app.shared.generated.resources.top_app_bar_back
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: la pantalla se afirma por aqui y no por su redaccion. */
object TrackerTags {
    const val FOLLOW_ROUTE: String = "tracker_follow_route"
    const val FOLLOWED_ROUTE: String = "tracker_followed_route"
    const val CLEAR_ROUTE: String = "tracker_clear_route"
    const val ROUTE_CHOICES: String = "tracker_route_choices"

    fun routeChoice(routeId: Long): String = "tracker_route_choice_$routeId"
}

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
    val followedRoute by viewModel.followedRoute.collectAsState()
    val savedRoutes by viewModel.savedRoutes.collectAsState()
    var pickingRoute by remember { mutableStateOf(false) }
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
            title = stringResource(Res.string.tracker_title),
            showBackButton = true,
            onBackClick = onBack,
            backContentDescription = stringResource(Res.string.top_app_bar_back),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            MovementMap(
                userLocation = userLocation,
                path = recording.traveledPoints,
                modifier = Modifier.fillMaxSize(),
                routePath = followedRoute?.points.orEmpty(),
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
                text = stringResource(
                    Res.string.tracker_session_saved,
                    MovementFormat.distance(saved.distanceMeters),
                    MovementFormat.duration(saved.elapsedSeconds),
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
                textAlign = TextAlign.Center,
            )
        }
        // Igual que el interruptor de la traza: la ruta pertenece a la salida que se va a empezar,
        // y ofrecer cambiarla a mitad de una grabacion seria ofrecer algo que no se puede hacer.
        FollowedRouteRow(
            route = followedRoute,
            isRecording = isRecording,
            onPick = { pickingRoute = true },
            onClear = viewModel::stopFollowingRoute,
        )

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

    if (pickingRoute) {
        PickRouteDialog(
            routes = savedRoutes,
            onDismiss = { pickingRoute = false },
            onPick = { routeId ->
                viewModel.followRoute(routeId)
                pickingRoute = false
            },
        )
    }
}

/**
 * Con que ruta se sale, o la puerta para elegir una.
 *
 * Mientras se graba solo se dice cual es: cambiarla a mitad de una salida no significa nada -la
 * sesion ya recuerda con cual empezo- y un control que no hace nada es peor que no tenerlo.
 */
@Composable
private fun FollowedRouteRow(
    route: FollowedRoute?,
    isRecording: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (route == null) {
            if (!isRecording) {
                TextButton(
                    onClick = onPick,
                    modifier = Modifier.testTag(TrackerTags.FOLLOW_ROUTE),
                ) {
                    Text(stringResource(Res.string.tracker_follow_route))
                }
            }
            return@Row
        }

        Text(
            text = stringResource(Res.string.tracker_following_route, route.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag(TrackerTags.FOLLOWED_ROUTE),
        )
        if (!isRecording) {
            TextButton(onClick = onClear, modifier = Modifier.testTag(TrackerTags.CLEAR_ROUTE)) {
                Text(stringResource(Res.string.tracker_stop_following_route))
            }
        }
    }
}

/**
 * Las rutas guardadas, para elegir con cual se sale.
 *
 * No importa un GPX desde aqui: importar ya vive en "Mis rutas", con su dialogo de duplicados y su
 * acceso a archivos por plataforma. Una segunda puerta a lo mismo seria el componente casi identico
 * que AGENTS.md llama fallo de diseno; el estado vacio dice donde se importa.
 */
@Composable
private fun PickRouteDialog(
    routes: List<Route>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.tracker_pick_route_title)) },
        text = {
            if (routes.isEmpty()) {
                Text(stringResource(Res.string.tracker_pick_route_empty))
            } else {
                LazyColumn(modifier = Modifier.testTag(TrackerTags.ROUTE_CHOICES)) {
                    items(routes, key = { it.id }) { route ->
                        Text(
                            text = route.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(route.id) }
                                .padding(vertical = HazloSpaces.sm)
                                .testTag(TrackerTags.routeChoice(route.id)),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
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
                text = stringResource(Res.string.tracker_trace_toggle_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.tracker_trace_toggle_description),
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
            Metric(
                label = stringResource(Res.string.movement_metric_distance),
                value = distance.shown(),
            )
            Metric(
                label = stringResource(Res.string.movement_metric_duration),
                value = MovementFormat.duration(elapsedSeconds),
            )
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
            Text(
                stringResource(
                    if (isRecording) Res.string.tracker_stop else Res.string.tracker_start,
                ),
            )
        }
        OutlinedButton(onClick = onOpenHistory) {
            Text(stringResource(Res.string.tracker_open_history))
        }
    }
}
