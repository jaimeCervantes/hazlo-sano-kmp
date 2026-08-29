package com.hazlosano.feature.catalog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.sections.HazloExploreProductsSection
import com.hazlosano.domain.model.PillarType
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.presentation.PillarCatalogViewModel
import com.hazlosano.feature.catalog.presentation.rememberPillarCatalogViewModel
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.catalog_empty
import hazlosano.app.shared.generated.resources.catalog_failed_message
import hazlosano.app.shared.generated.resources.catalog_failed_title
import hazlosano.app.shared.generated.resources.catalog_retry
import hazlosano.app.shared.generated.resources.catalog_search_placeholder
import hazlosano.app.shared.generated.resources.catalog_stale_notice
import hazlosano.app.shared.generated.resources.catalog_unavailable_message
import hazlosano.app.shared.generated.resources.catalog_unavailable_title
import org.jetbrains.compose.resources.stringResource

/**
 * El catálogo de un pilar.
 *
 * Una sola pantalla parametrizada en lugar de una por pilar: lo único que cambia entre Nutrición,
 * Movimiento y Mente es qué se pide y de qué color se pinta.
 */
@Composable
fun PillarCatalogScreen(
    pillar: PillarType,
    modifier: Modifier = Modifier,
    viewModel: PillarCatalogViewModel = rememberPillarCatalogViewModel(pillar),
) {
    val state by viewModel.uiState.collectAsState()
    val label = pillarLabel(pillar)
    val accent = pillarColor(pillar)

    Box(modifier = modifier.fillMaxSize()) {
        when (val current = state) {
            PillarCatalogUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = accent,
            )

            is PillarCatalogUiState.Ready -> Column(modifier = Modifier.fillMaxSize()) {
                if (current.fromCache) {
                    StaleNotice(message = stringResource(Res.string.catalog_stale_notice))
                }
                HazloExploreProductsSection(
                    products = current.publications,
                    modifier = Modifier.fillMaxSize(),
                    title = label,
                    placeholderText = stringResource(Res.string.catalog_search_placeholder, label),
                    emptyText = stringResource(Res.string.catalog_empty),
                    accentColor = accent,
                )
            }

            PillarCatalogUiState.Unavailable -> CatalogMessage(
                title = stringResource(Res.string.catalog_unavailable_title),
                message = stringResource(Res.string.catalog_unavailable_message),
                onRetry = viewModel::refresh,
                modifier = Modifier.align(Alignment.Center),
            )

            PillarCatalogUiState.Failed -> CatalogMessage(
                title = stringResource(Res.string.catalog_failed_title),
                message = stringResource(Res.string.catalog_failed_message),
                onRetry = viewModel::refresh,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/**
 * Lo viejo se enseña, pero se enseña **diciendo que es viejo**.
 *
 * Sin este aviso, un precio de hace una semana y uno de hace un segundo se ven igual, que es la
 * forma silenciosa de mentir que este slice vino a quitar.
 */
@Composable
private fun StaleNotice(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CatalogMessage(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(Res.string.catalog_retry))
        }
    }
}
