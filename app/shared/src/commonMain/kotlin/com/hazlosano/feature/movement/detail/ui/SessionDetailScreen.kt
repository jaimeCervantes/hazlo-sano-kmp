package com.hazlosano.feature.movement.detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarMovement
import com.hazlosano.feature.movement.detail.presentation.SessionDetailUi
import com.hazlosano.feature.movement.detail.presentation.SessionDetailUiState
import com.hazlosano.feature.movement.detail.presentation.SessionDiagnosisUi
import com.hazlosano.feature.movement.detail.presentation.createSessionDetailViewModel
import com.hazlosano.feature.movement.ui.MovementMap

private const val METRICS_PER_ROW = 4

/** Detail of one recorded session: its route on the map and the summary of what was recorded. */
@Composable
fun SessionDetailScreen(sessionId: Long, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = remember(sessionId) { createSessionDetailViewModel(sessionId) }
    val state by viewModel.state.collectAsState()
    val saveRouteMessage by viewModel.saveRouteMessage.collectAsState()
    var namingRoute by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val title = (state as? SessionDetailUiState.Detail)?.session?.name ?: "Sesión"
        HazloTopAppBar(title = title, showBackButton = true, onBackClick = onBack)

        if (viewModel.canSaveAsRoute && state is SessionDetailUiState.Detail) {
            TextButton(
                onClick = { namingRoute = true },
                modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
            ) {
                Text("Guardar como ruta")
            }
        }

        saveRouteMessage?.let { message ->
            Text(
                text = message,
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
                    CircularProgressIndicator(color = PillarMovement)
                }

                is SessionDetailUiState.Missing -> CenteredBox {
                    Message("Esta sesión ya no está disponible.")
                }

                is SessionDetailUiState.Error -> CenteredBox {
                    Message(current.message, color = MaterialTheme.colorScheme.error)
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
        title = { Text("Guardar como ruta") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Nombre de la ruta") },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
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
                    Message("Esta sesión no guardó el recorrido, así que no hay ruta que mostrar.")
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
                        SummaryMetric(label = metric.label, value = metric.value)
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
            text = if (expanded) "Diagnóstico ▾" else "Diagnóstico ▸",
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
                        text = row.label,
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
            color = PillarMovement,
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
