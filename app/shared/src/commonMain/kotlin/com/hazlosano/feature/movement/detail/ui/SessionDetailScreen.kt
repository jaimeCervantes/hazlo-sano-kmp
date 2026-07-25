package com.hazlosano.feature.movement.detail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarMovement
import com.hazlosano.feature.movement.detail.presentation.SessionDetailUi
import com.hazlosano.feature.movement.detail.presentation.SessionDetailUiState
import com.hazlosano.feature.movement.detail.presentation.createSessionDetailViewModel
import com.hazlosano.feature.movement.ui.MovementMap

/** Detail of one recorded session: its route on the map and the summary of what was recorded. */
@Composable
fun SessionDetailScreen(sessionId: Long, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = remember(sessionId) { createSessionDetailViewModel(sessionId) }
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val title = (state as? SessionDetailUiState.Detail)?.session?.name ?: "Sesión"
        HazloTopAppBar(title = title, showBackButton = true, onBackClick = onBack)

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

@Composable
private fun SessionSummary(session: SessionDetailUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SummaryMetric(label = "Distancia", value = session.distanceLabel)
        SummaryMetric(label = "Tiempo", value = session.durationLabel)
        SummaryMetric(label = "Ritmo", value = session.paceLabel)
        SummaryMetric(label = "Desnivel", value = session.elevationLabel)
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
