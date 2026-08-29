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
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarSleep
import com.hazlosano.core.ui.util.formatClockTime
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.domain.model.SleepContent
import com.hazlosano.domain.model.SleepSession
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.ui.CatalogStaleNotice
import com.hazlosano.feature.catalog.ui.pillarCatalogPlaceholder
import com.hazlosano.feature.catalog.ui.pillarCatalogSections
import com.hazlosano.feature.sleep.presentation.SleepUiState
import com.hazlosano.feature.sleep.presentation.SleepViewModel

/**
 * El pilar de sueño: su panel de análisis arriba y el catálogo del pilar debajo.
 *
 * Es la única pestaña de pilar que no usa `PillarCatalogScreen` entera, porque ya tiene un
 * encabezado propio —el resumen de la última noche, que es funcionalidad real— y un segundo héroe a
 * mitad de pantalla sobraría. Lo que sí comparte son las secciones del catálogo.
 *
 * [catalogState] entra por parámetro en lugar de construirse aquí para que la pantalla se pueda
 * componer en un test con un catálogo cualquiera.
 */
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    catalogState: PillarCatalogUiState,
    onRetryCatalog: () -> Unit,
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
            catalogState = catalogState,
            onRetryCatalog = onRetryCatalog,
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

/** Internal para poder componerlo en un test sin levantar un `SleepViewModel` entero. */
@Composable
internal fun SleepDashboardContent(
    content: SleepContent,
    sleepAnalysis: SleepAnalysis?,
    catalogState: PillarCatalogUiState,
    onRetryCatalog: () -> Unit,
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

        // El catálogo del pilar, ahora leído del sitio. Hasta aquí esta sección enseñaba una lista
        // escrita en `MockSleepRepository` —un antifaz a 18.0 que no existía en ninguna parte—, que
        // era la misma ficción que el seed que el catálogo remoto vino a quitar.
        if (catalogState is PillarCatalogUiState.Ready && catalogState.fromCache) {
            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
            item { CatalogStaleNotice() }
        }

        pillarCatalogPlaceholder(
            state = catalogState,
            accent = PillarSleep,
            onRetry = onRetryCatalog,
        )

        if (catalogState is PillarCatalogUiState.Ready) {
            pillarCatalogSections(pillar = PillarType.SLEEP, sections = catalogState.sections)
        }
    }
}