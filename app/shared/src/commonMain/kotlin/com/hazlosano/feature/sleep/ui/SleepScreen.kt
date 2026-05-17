package com.hazlosano.feature.sleep.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.atomic.SectionHeader
import com.hazlosano.core.ui.components.atomic.LeafCard
import com.hazlosano.core.ui.components.atomic.SleepSummaryCard
import com.hazlosano.core.ui.components.sections.HazloChallengesSection
import com.hazlosano.core.ui.components.sections.HazloChampionsSection
import com.hazlosano.core.ui.components.sections.HazloExploreProductsSection
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarSleep
import com.hazlosano.core.ui.util.formatClockTime
import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.domain.model.SleepContent
import com.hazlosano.domain.model.SleepSession
import com.hazlosano.feature.sleep.presentation.SleepUiState
import com.hazlosano.feature.sleep.presentation.SleepViewModel

@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    onRefresh: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is SleepUiState.Loading -> LoadingContent()
        is SleepUiState.Error -> ErrorContent(state.message)
        is SleepUiState.Success -> SleepDashboardContent(
            content = state.content,
            sleepAnalysis = state.sleepAnalysis,
            onRefresh = onRefresh,
            onCardClick = onCardClick ?: {},
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
    onRefresh: (() -> Unit)?,
    onCardClick: () -> Unit,
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
                    onClick = onCardClick,
                )
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