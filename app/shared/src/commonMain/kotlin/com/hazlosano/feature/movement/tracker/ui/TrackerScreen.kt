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
import hazlosano.app.shared.generated.resources.tracker_following_route
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
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
import com.hazlosano.core.ui.components.AppSettingsMenuItem
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.domain.model.PillarType
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.RouteStanding
import com.hazlosano.domain.feature.movement.model.goneNowhereMinutes
import com.hazlosano.feature.movement.presentation.MovementFormat
import com.hazlosano.feature.movement.tracker.presentation.TrackerDistance
import com.hazlosano.feature.movement.tracker.presentation.createTrackerViewModel
import com.hazlosano.feature.movement.tracker.presentation.trackerDistance
import com.hazlosano.feature.movement.ui.MovementBottomBar
import com.hazlosano.feature.movement.ui.MovementMap
import com.hazlosano.feature.movement.ui.MovementPlace
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.movement_metric_distance
import hazlosano.app.shared.generated.resources.movement_metric_moving_time
import hazlosano.app.shared.generated.resources.movement_metric_pace
import hazlosano.app.shared.generated.resources.movement_metric_duration
import hazlosano.app.shared.generated.resources.tracker_session_saved
import hazlosano.app.shared.generated.resources.tracker_start
import hazlosano.app.shared.generated.resources.tracker_stop
import hazlosano.app.shared.generated.resources.tracker_title
import hazlosano.app.shared.generated.resources.tracker_trace_toggle_description
import hazlosano.app.shared.generated.resources.tracker_trace_toggle_title
import hazlosano.app.shared.generated.resources.tracker_distance_confirming
import hazlosano.app.shared.generated.resources.tracker_distance_waiting_for_fix
import hazlosano.app.shared.generated.resources.tracker_gone_nowhere
import hazlosano.app.shared.generated.resources.tracker_off_route
import hazlosano.app.shared.generated.resources.top_app_bar_back
import hazlosano.app.shared.generated.resources.top_app_bar_menu
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: la pantalla se afirma por aqui y no por su redaccion. */
object TrackerTags {
    const val FOLLOWED_ROUTE: String = "tracker_followed_route"
    const val METRICS: String = "tracker_metrics"
    const val OFF_ROUTE: String = "tracker_off_route"
    const val SESSION_ACTION: String = "tracker_session_action"
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
    onOpenRoutes: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    /** La ruta con la que se llega, cuando se llega desde el detalle de una. */
    followRouteId: Long? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { createTrackerViewModel() }
    LocationPermissionEffect(onGranted = viewModel::startTracking)

    val userLocation by viewModel.userLocation.collectAsState()
    val followedRoute by viewModel.followedRoute.collectAsState()
    val liveStats by viewModel.liveStats.collectAsState()
    // **La ruta que se sigue la dice el destino, y nada mas.** Se elige en «Mis rutas», abriendo
    // una y pulsando Iniciar; aqui ya no hay con que elegirla, asi que esta pantalla se limita a
    // reflejar con que se llego.
    //
    // El `else` no sobra: volver al tracker a secas desde uno que seguia una ruta reusa la misma
    // composicion y, sin limpiar, la ruta anterior se quedaria pegada a una salida que no la sigue.
    LaunchedEffect(followRouteId) {
        if (followRouteId != null) viewModel.followRoute(followRouteId) else viewModel.stopFollowingRoute()
    }
    val routeStanding by viewModel.routeStanding.collectAsState()
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
            menuContentDescription = stringResource(Res.string.top_app_bar_menu),
            menuContent = { dismiss ->
                AppSettingsMenuItem(
                    onClick = {
                        dismiss()
                        onOpenSettings()
                    },
                )
            },
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
                stats = liveStats,
                modifier = Modifier.align(Alignment.TopStart).padding(HazloSpaces.gutter),
            )
            // Los dos avisos comparten sitio y no pueden salir juntos: el de desvio solo habla
            // mientras te mueves, y el de "llevas parado" solo cuando no. Se ponen en el mismo
            // orden que su urgencia, por si alguna vez esa invariante deja de cumplirse.
            (routeStanding as? RouteStanding.OffRoute)?.let { off ->
                OffRouteNotice(
                    metersFromRoute = off.metersFromRoute,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(HazloSpaces.gutter),
                )
            } ?: recording.goneNowhereMinutes()?.let { minutes ->
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
        FollowedRouteRow(route = followedRoute)

        // Only offered while idle: the choice applies to the recording being started, and showing a
        // switch that silently does nothing mid-session would be a lie.
        if (!isRecording) {
            TraceCaptureToggle(enabled = captureTrace, onChange = viewModel::setCaptureTrace)
        }
        SessionControls(
            isRecording = isRecording,
            onStart = viewModel::startRecording,
            onStop = viewModel::stopRecording,
        )
        MovementBottomBar(
            current = MovementPlace.Record,
            onGoToRecord = {},
            onGoToRoutes = onOpenRoutes,
            onGoToOutings = onOpenHistory,
        )
    }
}

/**
 * Con que ruta se sale, cuando se sale con una.
 *
 * **Solo lo dice; no deja elegirla.** Elegir ruta vive en «Mis rutas»: se abre la que se quiere y se
 * pulsa Iniciar. Tener aqui una segunda puerta a lo mismo era ofrecer dos caminos para una decision
 * que ya esta tomada al llegar, y obligaba a esta pantalla a llevar la lista entera de rutas para
 * un dialogo que nadie necesitaba abrir dos veces.
 *
 * Sigue estando porque llegar siguiendo una ruta y llegar sin ella son dos salidas distintas, y sin
 * este renglon no habria forma de saber cual de las dos se esta empezando hasta ver el trazado.
 */
@Composable
private fun FollowedRouteRow(route: FollowedRoute?) {
    if (route == null) return

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.tracker_following_route, route.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag(TrackerTags.FOLLOWED_ROUTE),
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
 * Te has salido de la ruta que llevabas.
 *
 * Dice **a cuanto** estas del trazado y no solo que te saliste: 60 m es volver sobre tus pasos un
 * minuto, y 800 m es otra decision. El numero es lo que convierte el aviso en algo accionable.
 *
 * Sale en rojo y no en el tono del pilar porque es lo unico de esta pantalla que pide hacer algo.
 */
@Composable
private fun OffRouteNotice(metersFromRoute: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag(TrackerTags.OFF_ROUTE),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
        tonalElevation = 3.dp,
    ) {
        Text(
            text = stringResource(
                Res.string.tracker_off_route,
                MovementFormat.distance(metersFromRoute),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = HazloSpaces.md, vertical = HazloSpaces.sm),
        )
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
    stats: SessionStats,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag(TrackerTags.METRICS),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 3.dp,
    ) {
        // Dos por dos y no cuatro en fila: cuatro cifras a lo ancho sobre un mapa de telefono se
        // comen la pantalla, y este bloque tapa el trazado que se ha venido a ver.
        Column(
            modifier = Modifier.padding(horizontal = HazloSpaces.md, vertical = HazloSpaces.sm),
            verticalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(HazloSpaces.md)) {
                Metric(
                    label = stringResource(Res.string.movement_metric_distance),
                    value = distance.shown(),
                )
                Metric(
                    label = stringResource(Res.string.movement_metric_duration),
                    value = MovementFormat.duration(elapsedSeconds),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(HazloSpaces.md)) {
                Metric(
                    label = stringResource(Res.string.movement_metric_pace),
                    value = MovementFormat.pace(stats.avgPace),
                )
                Metric(
                    label = stringResource(Res.string.movement_metric_moving_time),
                    value = MovementFormat.duration(stats.movingTime),
                )
            }
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

/**
 * Una cifra del directo.
 *
 * **Ancho minimo y un solo renglon**, las dos cosas a proposito: el valor cambia con cada lectura
 * -«0:00» pasa a «10:32», «— » pasa a «7:25 /km»- y sin un ancho de suelo el bloque entero se movia
 * bajo el dedo cada dos segundos. Que no parta en dos renglones es lo mismo por el otro lado: la
 * altura del bloque no puede depender de lo que valga un numero.
 */
@Composable
private fun Metric(label: String, value: String) {
    Column(modifier = Modifier.widthIn(min = METRIC_MIN_WIDTH)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Lo ancho que hay que reservar para que «7:25 /km» y «En movimiento» quepan sin bailar. */
private val METRIC_MIN_WIDTH = 96.dp

@Composable
private fun SessionControls(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    // Solo la accion. El «Mis salidas» delineado que iba aqui al lado era una navegacion puesta
    // junto a una accion, y se ha ido a la barra de abajo con los otros dos sitios del pilar.
    Row(
        modifier = Modifier.fillMaxWidth().padding(HazloSpaces.gutter),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = if (isRecording) onStop else onStart,
            modifier = Modifier.fillMaxWidth().testTag(TrackerTags.SESSION_ACTION),
            shape = RoundedCornerShape(HazloShapes.control),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error
                } else {
                    PillarType.MOVEMENT.palette().solid
                },
            ),
        ) {
            Text(
                stringResource(
                    if (isRecording) Res.string.tracker_stop else Res.string.tracker_start,
                ),
            )
        }
    }
}
