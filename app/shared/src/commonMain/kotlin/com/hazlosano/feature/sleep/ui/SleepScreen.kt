package com.hazlosano.feature.sleep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.hazlosano.core.ui.components.atomic.SleepSummaryCard
import com.hazlosano.core.ui.components.sections.pillarHighlightsSections
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarSleep
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.ui.CatalogStaleNotice
import com.hazlosano.feature.catalog.ui.pillarCatalogPlaceholder
import com.hazlosano.feature.catalog.ui.pillarCatalogSections
import com.hazlosano.feature.pillar.presentation.PillarHighlightsUiState
import com.hazlosano.feature.sleep.presentation.SleepUiState
import com.hazlosano.feature.sleep.presentation.SleepViewModel

/**
 * El pilar de sueño: su panel de análisis arriba, la comunidad en medio y el catálogo debajo.
 *
 * Es la única pestaña de pilar que no usa `PillarCatalogScreen` entera, porque ya tiene un
 * encabezado propio —el resumen de la última noche, que es funcionalidad real— y un segundo héroe a
 * mitad de pantalla sobraría. Lo que sí comparte es el orden: campeones, retos y, al final, buscar.
 *
 * [highlights] y [catalogState] entran por parámetro en lugar de construirse aquí para que la
 * pantalla se pueda componer en un test con cualquier combinación de los dos.
 */
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    highlights: PillarHighlightsUiState,
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
            sleepAnalysis = state.sleepAnalysis,
            highlights = highlights,
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
    sleepAnalysis: SleepAnalysis?,
    highlights: PillarHighlightsUiState,
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
        }

        // Campeones y retos salen ahora de la misma fuente que los de los otros tres pilares.
        // Antes vivían escritos dentro de `MockSleepRepository`, que era la única copia de un
        // contenido que los cuatro pilares necesitaban igual.
        if (highlights is PillarHighlightsUiState.Ready) {
            pillarHighlightsSections(highlights = highlights.highlights, accent = PillarSleep)
        }

        // El catálogo del pilar, leído del sitio. Hasta la migración esta sección enseñaba una lista
        // escrita en el código —un antifaz a 18.0 que no existía en ninguna parte—, que era la misma
        // ficción que el seed que el catálogo remoto vino a quitar.
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
