package com.hazlosano.feature.movement.detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hazlosano.core.ui.components.AppSettingsMenuItem
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.model.PillarType
import com.hazlosano.feature.movement.detail.presentation.SessionDetailUi
import com.hazlosano.feature.movement.detail.presentation.SessionDetailUiState
import com.hazlosano.feature.movement.detail.presentation.SessionDiagnosisUi
import com.hazlosano.feature.movement.detail.presentation.createSessionDetailViewModel
import com.hazlosano.feature.movement.ui.MovementMap
import hazlosano.app.shared.generated.resources.Res
import com.hazlosano.feature.movement.ui.label
import com.hazlosano.feature.movement.ui.text
import hazlosano.app.shared.generated.resources.action_cancel
import hazlosano.app.shared.generated.resources.action_save
import hazlosano.app.shared.generated.resources.session_detail_diagnosis_collapsed
import hazlosano.app.shared.generated.resources.session_detail_diagnosis_expanded
import hazlosano.app.shared.generated.resources.session_detail_failed
import hazlosano.app.shared.generated.resources.session_detail_fallback_title
import hazlosano.app.shared.generated.resources.session_detail_missing
import hazlosano.app.shared.generated.resources.session_detail_no_path
import hazlosano.app.shared.generated.resources.session_detail_route_name_label
import hazlosano.app.shared.generated.resources.session_detail_save_as_route
import hazlosano.app.shared.generated.resources.top_app_bar_back
import hazlosano.app.shared.generated.resources.top_app_bar_menu
import org.jetbrains.compose.resources.stringResource

private const val METRICS_PER_ROW = 4

/** Etiquetas de prueba: la pantalla se afirma por aqui y no por su redaccion. */
object SessionDetailTags {
    const val SAVE_AS_ROUTE: String = "session_detail_save_as_route"
}

/** Detail of one recorded session: its route on the map and the summary of what was recorded. */
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel = remember(sessionId) { createSessionDetailViewModel(sessionId) }
    val state by viewModel.state.collectAsState()
    val saveRouteMessage by viewModel.saveRouteMessage.collectAsState()
    var namingRoute by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val title = (state as? SessionDetailUiState.Detail)?.session?.name
            ?: stringResource(Res.string.session_detail_fallback_title)
        HazloTopAppBar(
            title = title,
            showBackButton = true,
            onBackClick = onBack,
            backContentDescription = stringResource(Res.string.top_app_bar_back),
            menuContentDescription = stringResource(Res.string.top_app_bar_menu),
            // Guardar como ruta estaba suelto bajo la barra, dentro del contenido. Es una accion
            // sobre la salida que se esta mirando, asi que va donde van las acciones desde el
            // slice 3 — y con la misma forma que el detalle de una ruta: lo de esto que miras, una
            // raya, y Ajustes.
            menuContent = { dismiss ->
                SessionDetailMenuItems(
                    canSaveAsRoute = viewModel.canSaveAsRoute &&
                        state is SessionDetailUiState.Detail,
                    onSaveAsRoute = {
                        dismiss()
                        namingRoute = true
                    },
                    onOpenSettings = {
                        dismiss()
                        onOpenSettings()
                    },
                )
            },
        )

        saveRouteMessage?.let { message ->
            Text(
                text = message.text(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.sm),
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val current = state) {
                is SessionDetailUiState.Loading -> CenteredBox {
                    CircularProgressIndicator(color = PillarType.MOVEMENT.palette().ink)
                }

                is SessionDetailUiState.Missing -> CenteredBox {
                    Message(stringResource(Res.string.session_detail_missing))
                }

                SessionDetailUiState.Error -> CenteredBox {
                    Message(
                        stringResource(Res.string.session_detail_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                is SessionDetailUiState.Detail -> SessionDetailContent(current.session)
            }
        }
    }

    if (namingRoute) {
        val suggested = (state as? SessionDetailUiState.Detail)?.session?.name.orEmpty()
        NameRouteDialog(
            suggestedName = suggested,
            onDismiss = { namingRoute = false },
            onConfirm = { name ->
                viewModel.consumeSaveRouteMessage()
                viewModel.saveAsRoute(name)
                namingRoute = false
            },
        )
    }
}

/**
 * The name is asked for rather than taken from the session: "Salida del 9 ago" is what happened
 * that day, not what the route is called. The session name is offered as a starting point.
 */
@Composable
private fun NameRouteDialog(
    suggestedName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(suggestedName) { mutableStateOf(suggestedName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.session_detail_save_as_route)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(Res.string.session_detail_route_name_label)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

@Composable
private fun SessionDetailContent(session: SessionDetailUi) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = session.dateLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.sm),
        )

        SessionSummary(session)

        session.diagnosis?.let { Diagnosis(it) }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (session.hasPath) {
                MovementMap(
                    userLocation = null,
                    path = session.path,
                    modifier = Modifier.fillMaxSize(),
                    fitPathInView = true,
                )
            } else {
                CenteredBox {
                    Message(stringResource(Res.string.session_detail_no_path))
                }
            }
        }
    }
}

/**
 * Laid out as rows of four rather than one row of eight: eight figures across a phone would shrink
 * every one of them until none could be read at a glance, and the map still needs the room.
 */
@Composable
private fun SessionSummary(session: SessionDetailUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.sm),
        verticalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
    ) {
        session.metrics.chunked(METRICS_PER_ROW).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { metric ->
                    Box(modifier = Modifier.weight(1f)) {
                        SummaryMetric(label = metric.metric.label(), value = metric.value)
                    }
                }
                // Keeps a short last row aligned with the one above instead of spreading out.
                repeat(METRICS_PER_ROW - row.size) {
                    Box(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

/**
 * Collapsed by default: the diagnosis explains a distance that looks wrong, so it belongs next to
 * the metrics — but it is for calibrating the filter, not for reading about an outing.
 */
@Composable
private fun Diagnosis(diagnosis: SessionDiagnosisUi) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = HazloSpaces.gutter)) {
        Text(
            text = stringResource(
                if (expanded) {
                    Res.string.session_detail_diagnosis_expanded
                } else {
                    Res.string.session_detail_diagnosis_collapsed
                },
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable { expanded = !expanded }
                .fillMaxWidth()
                .padding(vertical = HazloSpaces.sm),
        )

        if (expanded) {
            diagnosis.rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = HazloSpaces.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = row.label.text(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PillarType.MOVEMENT.palette().ink,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(HazloSpaces.gutter),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Message(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Center,
    )
}

/**
 * Lo que se puede hacer con la salida que se esta mirando.
 *
 * **La misma forma que el detalle de una ruta**: primero lo que se le hace a esto que miras, luego
 * una raya, luego lo del app. Que las dos pantallas de detalle se lean igual es lo que evita tener
 * que aprenderse cada una.
 *
 * «Guardar como ruta» vivia suelto bajo la barra, dentro del contenido, y es una accion sobre esta
 * salida: va donde van las acciones desde el slice 3.
 *
 * Sale con nombre propio y no escrito dentro de la barra para poder componerlo en un test: la
 * pantalla entera construye su ViewModel con la persistencia real y no se puede levantar en la JVM.
 *
 * @param canSaveAsRoute una salida sin recorrido no puede ser una ruta, y este target puede no tener
 * donde guardarla. Lo del app no depende de ninguna de las dos cosas, asi que Ajustes esta siempre.
 */
@Composable
fun ColumnScope.SessionDetailMenuItems(
    canSaveAsRoute: Boolean,
    onSaveAsRoute: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (canSaveAsRoute) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.session_detail_save_as_route)) },
            leadingIcon = { Icon(Icons.Default.AddRoad, contentDescription = null) },
            onClick = onSaveAsRoute,
            modifier = Modifier.testTag(SessionDetailTags.SAVE_AS_ROUTE),
        )
    }
    AppSettingsMenuItem(onClick = onOpenSettings, afterOtherItems = canSaveAsRoute)
}
