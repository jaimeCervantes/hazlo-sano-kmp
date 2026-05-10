package com.hazlosano.kmp.feature.sleep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazlosano.kmp.core.ui.components.atomic.SectionHeader
import com.hazlosano.kmp.core.ui.components.atomic.LeafCard
import com.hazlosano.kmp.core.ui.components.atomic.SleepSummaryCard
import com.hazlosano.kmp.core.ui.components.sections.HazloChallengesSection
import com.hazlosano.kmp.core.ui.components.sections.HazloChampionsSection
import com.hazlosano.kmp.core.ui.components.sections.HazloExploreProductsSection
import com.hazlosano.kmp.core.ui.theme.HazloSpaces
import com.hazlosano.kmp.core.ui.theme.PillarSleep
import com.hazlosano.kmp.core.ui.util.formatClockTime
import com.hazlosano.kmp.domain.model.SleepAnalysis
import com.hazlosano.kmp.domain.model.SleepContent
import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.feature.sleep.presentation.SleepUiState
import com.hazlosano.kmp.feature.sleep.presentation.SleepViewModel

@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    onRefresh: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    when (val state = uiState) {
        is SleepUiState.Loading -> LoadingContent()
        is SleepUiState.Error -> ErrorContent(state.message)
        is SleepUiState.Success -> SleepDashboardContent(
            content = state.content,
            sleepAnalysis = state.sleepAnalysis,
            sessions = sessions,
            onRefresh = onRefresh,
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = PillarSleep)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SleepDashboardContent(
    content: SleepContent,
    sleepAnalysis: SleepAnalysis?,
    sessions: List<SleepSession>,
    onRefresh: (() -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = HazloSpaces.default, bottom = HazloSpaces.xl),
    ) {
        if (sleepAnalysis != null) {
            item {
                SleepSummaryCard(
                    analysis = sleepAnalysis,
                    accentColor = PillarSleep,
                    modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
                    onRefresh = onRefresh,
                )
            }
            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
        }

        if (sessions.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Segmentos detectados",
                    modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
                    accentColor = PillarSleep,
                    actionText = null,
                )
            }
            items(sessions.size) { index ->
                val session = sessions[index]
                LeafCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HazloSpaces.gutter, vertical = 4.dp),
                ) {
                    Column(modifier = Modifier.padding(HazloSpaces.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${formatClockTime(session.startTime)} → ${formatClockTime(session.endTime)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = session.phase.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = PillarSleep,
                            )
                        }
                        Text(
                            text = "Duración: ${session.duration / 60_000} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
        }

        item {
            HazloChampionsSection(
                champions = content.weeklyChampions,
                title = "Campeones Semanales",
                accentColor = PillarSleep,
            )
        }

        item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }

        item {
            HazloChallengesSection(
                challenges = content.activeChallenges,
                accentColor = PillarSleep,
            )
        }

        item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }

        item {
            HazloExploreProductsSection(
                products = content.productsAndServices,
                placeholderText = "Antifaz, consulta, almohada...",
                emptyText = "No hay productos o servicios de sueño con ese nombre.",
                modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
                accentColor = PillarSleep,
            )
        }
    }
}