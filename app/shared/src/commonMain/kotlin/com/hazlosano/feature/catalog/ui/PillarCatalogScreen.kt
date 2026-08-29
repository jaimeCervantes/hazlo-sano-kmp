package com.hazlosano.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.atomic.HazloAsyncImage
import com.hazlosano.core.ui.components.atomic.SectionHeader
import com.hazlosano.core.ui.components.cards.HazloProductCard
import com.hazlosano.core.ui.components.cards.HazloProductCardImagePlaceholder
import com.hazlosano.core.ui.components.sections.HazloExploreProductsSection
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.core.ui.util.formatClockTime
import com.hazlosano.core.ui.util.formatDate
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.PublicationKind
import com.hazlosano.feature.catalog.presentation.CatalogSections
import com.hazlosano.feature.catalog.presentation.PillarCatalogUiState
import com.hazlosano.feature.catalog.presentation.PillarCatalogViewModel
import com.hazlosano.feature.catalog.presentation.rememberPillarCatalogViewModel
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.catalog_empty
import hazlosano.app.shared.generated.resources.catalog_failed_message
import hazlosano.app.shared.generated.resources.catalog_failed_title
import hazlosano.app.shared.generated.resources.catalog_metric_events
import hazlosano.app.shared.generated.resources.catalog_metric_publications
import hazlosano.app.shared.generated.resources.catalog_metric_services
import hazlosano.app.shared.generated.resources.catalog_refresh
import hazlosano.app.shared.generated.resources.catalog_retry
import hazlosano.app.shared.generated.resources.catalog_search_placeholder
import hazlosano.app.shared.generated.resources.catalog_section_all
import hazlosano.app.shared.generated.resources.catalog_section_nearby
import hazlosano.app.shared.generated.resources.catalog_section_services
import hazlosano.app.shared.generated.resources.catalog_section_upcoming_events
import hazlosano.app.shared.generated.resources.catalog_stale_notice
import hazlosano.app.shared.generated.resources.catalog_unavailable_message
import hazlosano.app.shared.generated.resources.catalog_unavailable_title
import hazlosano.app.shared.generated.resources.publication_duration_minutes
import hazlosano.app.shared.generated.resources.publication_price_free
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba. La estructura del tablero se afirma por aquí y no por su redacción. */
object PillarCatalogTags {
    const val SUMMARY: String = "catalog_summary"
    const val STALE_NOTICE: String = "catalog_stale_notice"
    const val EVENTS: String = "catalog_events"
    const val SERVICES: String = "catalog_services"
    const val NEARBY: String = "catalog_nearby"
    const val GRID: String = "catalog_grid"
    const val UNAVAILABLE: String = "catalog_unavailable"
}

/**
 * El catálogo de un pilar, cableado a su ViewModel.
 *
 * Una sola pantalla parametrizada: lo único que cambia entre Nutrición, Movimiento y Mente es qué
 * se pide y de qué color se pinta.
 */
@Composable
fun PillarCatalogScreen(
    pillar: PillarType,
    modifier: Modifier = Modifier,
    viewModel: PillarCatalogViewModel = rememberPillarCatalogViewModel(pillar),
) {
    val state by viewModel.uiState.collectAsState()

    PillarCatalogContent(
        pillar = pillar,
        state = state,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

/**
 * El tablero sin ViewModel, para poder componerlo en un test con un estado cualquiera.
 *
 * Mismo esqueleto que la pantalla de sueño: un `LazyColumn` de secciones separadas por
 * `HazloSpaces.md`, resumen arriba y carruseles debajo.
 */
@Composable
fun PillarCatalogContent(
    pillar: PillarType,
    state: PillarCatalogUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = pillarColor(pillar)

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state) {
            PillarCatalogUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = accent,
            )

            is PillarCatalogUiState.Ready -> PillarBoard(
                pillar = pillar,
                sections = state.sections,
                fromCache = state.fromCache,
                onRefresh = onRetry,
            )

            PillarCatalogUiState.Unavailable -> CatalogMessage(
                title = stringResource(Res.string.catalog_unavailable_title),
                message = stringResource(Res.string.catalog_unavailable_message),
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center).testTag(PillarCatalogTags.UNAVAILABLE),
            )

            PillarCatalogUiState.Failed -> CatalogMessage(
                title = stringResource(Res.string.catalog_failed_title),
                message = stringResource(Res.string.catalog_failed_message),
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun PillarBoard(
    pillar: PillarType,
    sections: CatalogSections,
    fromCache: Boolean,
    onRefresh: () -> Unit,
) {
    val label = pillarLabel(pillar)
    val accent = pillarColor(pillar)
    val icon = pillarIcon(pillar)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = HazloSpaces.default, bottom = HazloSpaces.xl),
    ) {
        // El aviso va encima de todo: enterarse de que el tablero es viejo después de leerlo no
        // sirve de nada.
        if (fromCache) {
            item { StaleNotice(message = stringResource(Res.string.catalog_stale_notice)) }
            item { Spacer(modifier = Modifier.height(HazloSpaces.sm)) }
        }

        item {
            PillarSummaryCard(
                title = label,
                icon = icon,
                accentColor = accent,
                metrics = listOf(
                    PillarMetric(
                        value = sections.total.toString(),
                        label = stringResource(Res.string.catalog_metric_publications),
                    ),
                    PillarMetric(
                        value = sections.eventCount.toString(),
                        label = stringResource(Res.string.catalog_metric_events),
                    ),
                    PillarMetric(
                        value = sections.serviceCount.toString(),
                        label = stringResource(Res.string.catalog_metric_services),
                    ),
                ),
                refreshContentDescription = stringResource(Res.string.catalog_refresh),
                onRefresh = onRefresh,
                modifier = Modifier
                    .padding(horizontal = HazloSpaces.gutter)
                    .testTag(PillarCatalogTags.SUMMARY),
            )
        }

        // Cada carrusel se dibuja solo si tiene algo. Un encabezado sobre una fila vacía es peor
        // que la ausencia de la sección: promete contenido que no llega.
        publicationCarousel(
            tag = PillarCatalogTags.EVENTS,
            titleRes = Res.string.catalog_section_upcoming_events,
            publications = sections.upcomingEvents,
            accent = accent,
        )
        publicationCarousel(
            tag = PillarCatalogTags.SERVICES,
            titleRes = Res.string.catalog_section_services,
            publications = sections.services,
            accent = accent,
        )
        publicationCarousel(
            tag = PillarCatalogTags.NEARBY,
            titleRes = Res.string.catalog_section_nearby,
            publications = sections.nearby,
            accent = accent,
        )

        item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }

        item {
            HazloExploreProductsSection(
                products = sections.all,
                modifier = Modifier
                    .padding(horizontal = HazloSpaces.gutter)
                    .testTag(PillarCatalogTags.GRID),
                title = stringResource(Res.string.catalog_section_all),
                placeholderText = stringResource(Res.string.catalog_search_placeholder, label),
                emptyText = stringResource(Res.string.catalog_empty),
                accentColor = accent,
            )
        }
    }
}

/**
 * Un carrusel horizontal de publicaciones, con el mismo ritmo que las secciones de sueño:
 * encabezado con margen de gutter, y la fila sangrando hasta el borde.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.publicationCarousel(
    tag: String,
    titleRes: org.jetbrains.compose.resources.StringResource,
    publications: List<HazloProduct>,
    accent: androidx.compose.ui.graphics.Color,
) {
    if (publications.isEmpty()) return

    item { Spacer(modifier = Modifier.height(HazloSpaces.md)) }
    item {
        Column(modifier = Modifier.testTag(tag)) {
            SectionHeader(
                title = stringResource(titleRes),
                modifier = Modifier.padding(horizontal = HazloSpaces.gutter),
                actionText = null,
                accentColor = accent,
            )
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(HazloSpaces.md),
                contentPadding = PaddingValues(horizontal = HazloSpaces.gutter),
            ) {
                items(publications, key = { it.id }) { publication ->
                    CarouselCard(publication = publication, accent = accent)
                }
            }
        }
    }
}

/**
 * La misma `HazloProductCard` de la rejilla, con ancho fijo.
 *
 * Una tarjeta aparte para el carrusel habría sido el segundo componente casi idéntico que
 * `AGENTS.md` llama fallo de diseño; lo único que el carrusel necesita de verdad es un ancho.
 */
@Composable
private fun CarouselCard(publication: HazloProduct, accent: androidx.compose.ui.graphics.Color) {
    HazloProductCard(
        title = publication.name,
        description = publication.description,
        price = publication.price,
        priceFallbackLabel = stringResource(Res.string.publication_price_free)
            .takeIf { publication.kind == PublicationKind.EVENT },
        overlineLabel = publication.overlineLabel(),
        isFavorite = publication.isFavorite,
        distanceMeters = publication.distanceMeters,
        onFavoriteClick = {},
        accentColor = accent,
        modifier = Modifier.width(CAROUSEL_CARD_WIDTH),
        imageContent = {
            if (publication.imageUrl.isBlank()) {
                HazloProductCardImagePlaceholder(title = publication.name, accentColor = accent)
            } else {
                HazloAsyncImage(
                    model = publication.imageUrl,
                    contentDescription = publication.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}

/**
 * Qué dice la línea de encima: cuándo ocurre un evento, cuánto dura un servicio, nada en lo demás.
 *
 * La fecha se compone con los formateadores que ya existían en `core/ui/util`; escribir otro aquí
 * habría dado dos formas distintas de pintar el mismo día en la misma app.
 */
@Composable
private fun HazloProduct.overlineLabel(): String? = when {
    kind == PublicationKind.EVENT && startsAtEpochMillis != null ->
        "${formatDate(startsAtEpochMillis!!)} · ${formatClockTime(startsAtEpochMillis!!)}"

    kind == PublicationKind.SERVICE && durationMinutes != null ->
        stringResource(Res.string.publication_duration_minutes, durationMinutes!!)

    else -> null
}

@Composable
private fun StaleNotice(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(PillarCatalogTags.STALE_NOTICE),
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
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
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

private val CAROUSEL_CARD_WIDTH = 170.dp
