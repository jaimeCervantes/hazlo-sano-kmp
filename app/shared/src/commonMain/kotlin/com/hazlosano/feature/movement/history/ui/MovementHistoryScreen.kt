package com.hazlosano.feature.movement.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.components.atomic.LeafCard
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarMovement
import com.hazlosano.feature.movement.history.presentation.MovementHistoryUiState
import com.hazlosano.feature.movement.history.presentation.SessionListItem
import com.hazlosano.feature.movement.history.presentation.createMovementHistoryViewModel
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.top_app_bar_back
import org.jetbrains.compose.resources.stringResource

/** Lists the sessions recorded with the tracker, newest first. */
@Composable
fun MovementHistoryScreen(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onOpenRoutes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { createMovementHistoryViewModel() }
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(
            title = "Historial",
            showBackButton = true,
            onBackClick = onBack,
            backContentDescription = stringResource(Res.string.top_app_bar_back),
        )

        TextButton(
            onClick = onOpenRoutes,
            modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
        ) {
            Text("Mis rutas")
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val current = state) {
                is MovementHistoryUiState.Loading -> CenteredBox {
                    CircularProgressIndicator(color = PillarMovement)
                }

                is MovementHistoryUiState.Empty -> CenteredBox {
                    Message("Aún no has grabado sesiones. Pulsa \"Iniciar\" en el mapa para grabar la primera.")
                }

                is MovementHistoryUiState.Error -> CenteredBox {
                    Message(current.message, color = MaterialTheme.colorScheme.error)
                }

                is MovementHistoryUiState.Sessions -> SessionList(current.items, onOpenSession)
            }
        }
    }
}

@Composable
private fun SessionList(items: List<SessionListItem>, onOpenSession: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HazloSpaces.gutter,
            end = HazloSpaces.gutter,
            top = HazloSpaces.md,
            bottom = HazloSpaces.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
    ) {
        items(items, key = { it.id }) { item ->
            SessionRow(item, onClick = { onOpenSession(item.id) })
        }
    }
}

@Composable
private fun SessionRow(item: SessionListItem, onClick: () -> Unit) {
    LeafCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(HazloSpaces.md)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.dateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = HazloSpaces.sm),
                horizontalArrangement = Arrangement.spacedBy(HazloSpaces.lg),
            ) {
                SessionMetric(label = "Distancia", value = item.distanceLabel)
                SessionMetric(label = "Tiempo", value = item.durationLabel)
            }
        }
    }
}

@Composable
private fun SessionMetric(label: String, value: String) {
    Column {
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
