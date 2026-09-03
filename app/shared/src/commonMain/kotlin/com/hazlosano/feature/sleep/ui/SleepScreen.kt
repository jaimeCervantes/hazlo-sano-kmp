package com.hazlosano.feature.sleep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.atomic.HazloSkeleton
import com.hazlosano.core.ui.components.atomic.SleepSummaryCard
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.theme.PillarSleep
import com.hazlosano.domain.model.PillarType
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.ui.CatalogStaleNotice
import com.hazlosano.feature.catalog.ui.pillarCatalogPlaceholder
import com.hazlosano.feature.catalog.ui.pillarCatalogSections
import com.hazlosano.feature.pillar.presentation.PillarHighlightsUiState
import com.hazlosano.feature.pillar.ui.pillarHighlights
import com.hazlosano.feature.sleep.presentation.SleepUiState
import com.hazlosano.feature.sleep.presentation.SleepViewModel
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.action_retry
import hazlosano.app.shared.generated.resources.pillar_info_open
import hazlosano.app.shared.generated.resources.sleep_failed_message
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba de la pestaña de sueño. */
object SleepTags {
    const val SUMMARY_GAP: String = "skeleton_sleep_summary"
    const val FAILED: String = "sleep_failed"
    const val RETRY: String = "sleep_retry"
}

/**
 * El pilar de sueño: su panel de análisis arriba, la comunidad en medio y el catálogo debajo.
 *
 * Es la única pestaña de pilar que no usa `PillarCatalogScreen` entera, porque ya tiene un
 * encabezado propio —el resumen de la última noche, que es funcionalidad real— y un segundo héroe a
 * mitad de pantalla sobraría. Lo que sí comparte es el orden: campeones, retos y, al final, buscar.
 *
 * [highlights] y [catalogState] entran por parámetro en lugar de construirse aquí para que la
 * pantalla se pueda componer en un test con cualquier combinación de los tres estados.
 */
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    highlights: PillarHighlightsUiState,
    catalogState: PillarCatalogUiState,
    onRetryCatalog: () -> Unit,
    onOpenInfo: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    SleepDashboardContent(
        state = uiState,
        highlights = highlights,
        catalogState = catalogState,
        onRetryCatalog = onRetryCatalog,
        onOpenInfo = onOpenInfo,
        onRefresh = onRefresh,
        onCardClick = onCardClick ?: {},
        onRetry = viewModel::refresh,
    )
}

/**
 * Internal para poder componerlo en un test sin levantar un `SleepViewModel` entero.
 *
 * Las tres esperas de esta pantalla —el análisis de anoche, los campeones y el catálogo— son
 * independientes, y cada una enseña lo suyo. Antes, mientras se calculaba el análisis, un
 * `LoadingContent` a pantalla completa tapaba también los campeones y el catálogo, que ya estaban.
 */
@Composable
internal fun SleepDashboardContent(
    state: SleepUiState,
    highlights: PillarHighlightsUiState,
    catalogState: PillarCatalogUiState,
    onRetryCatalog: () -> Unit,
    onRefresh: (() -> Unit)?,
    onCardClick: () -> Unit,
    /** Por defecto no lleva a ninguna parte, para poder componer el panel suelto en un test. */
    onOpenInfo: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = HazloSpaces.default, bottom = HazloSpaces.xl),
    ) {
        sleepSummary(
            state = state,
            onRefresh = onRefresh,
            onCardClick = onCardClick,
            onOpenInfo = onOpenInfo,
            onRetry = onRetry,
        )

        // Campeones y retos salen ahora de la misma fuente que los de los otros tres pilares.
        // Antes vivían escritos dentro de `MockSleepRepository`, que era la única copia de un
        // contenido que los cuatro pilares necesitaban igual.
        pillarHighlights(state = highlights, accent = PillarSleep)

        // El catálogo del pilar, leído del sitio. Hasta la migración esta sección enseñaba una lista
        // escrita en el código —un antifaz a 18.0 que no existía en ninguna parte—, que era la misma
        // ficción que el seed que el catálogo remoto vino a quitar.
        if (catalogState is PillarCatalogUiState.Ready && catalogState.fromCache) {
            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
            item { CatalogStaleNotice() }
        }

        pillarCatalogPlaceholder(state = catalogState, onRetry = onRetryCatalog, onOpenInfo = onOpenInfo)

        if (catalogState is PillarCatalogUiState.Ready) {
            pillarCatalogSections(pillar = PillarType.SLEEP, sections = catalogState.sections)
        }
    }
}

/**
 * El resumen de anoche: la tarjeta cuando hay noche que contar, su hueco mientras se calcula.
 *
 * Una noche sin datos no deja hueco: no es que esté cargando, es que no hay nada que enseñar
 * todavía, y un hueco eterno haría creer lo contrario.
 */
private fun LazyListScope.sleepSummary(
    state: SleepUiState,
    onRefresh: (() -> Unit)?,
    onCardClick: () -> Unit,
    onOpenInfo: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        SleepUiState.Loading -> {
            item {
                HazloSkeleton(
                    modifier = Modifier
                        .padding(horizontal = HazloSpaces.gutter)
                        .fillMaxWidth()
                        .height(SUMMARY_HEIGHT)
                        .testTag(SleepTags.SUMMARY_GAP),
                    shape = RoundedCornerShape(HazloShapes.xl),
                )
            }
            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
        }

        SleepUiState.Failed -> item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.sleep_failed_message),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HazloSpaces.md)
                        .testTag(SleepTags.FAILED),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onRetry, modifier = Modifier.testTag(SleepTags.RETRY)) {
                    Text(stringResource(Res.string.action_retry))
                }
            }
        }

        is SleepUiState.Success -> state.sleepAnalysis?.let { analysis ->
            item {
                SleepSummaryCard(
                    analysis = analysis,
                    accentColor = PillarSleep,
                    modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
                    onRefresh = onRefresh,
                    onClick = onCardClick,
                    infoContentDescription = stringResource(Res.string.pillar_info_open),
                    onInfo = onOpenInfo,
                )
            }
            item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
        }
    }
}

private val SUMMARY_HEIGHT: Dp = 240.dp
