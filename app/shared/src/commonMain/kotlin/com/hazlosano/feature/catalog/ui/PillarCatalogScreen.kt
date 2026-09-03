package com.hazlosano.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.hazlosano.core.ui.components.sections.pillarCatalogSkeleton
import com.hazlosano.core.ui.components.sections.pillarSummarySkeleton
import com.hazlosano.core.ui.model.pillarIcon
import com.hazlosano.core.ui.model.pillarLabel
import com.hazlosano.core.ui.model.toColor
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
import com.hazlosano.feature.pillar.presentation.PillarHighlightsUiState
import com.hazlosano.feature.pillar.presentation.PillarHighlightsViewModel
import com.hazlosano.feature.pillar.presentation.rememberPillarHighlightsViewModel
import com.hazlosano.feature.pillar.ui.pillarHighlights
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.action_retry
import hazlosano.app.shared.generated.resources.catalog_empty
import hazlosano.app.shared.generated.resources.catalog_failed_message
import hazlosano.app.shared.generated.resources.catalog_failed_title
import hazlosano.app.shared.generated.resources.catalog_metric_events
import hazlosano.app.shared.generated.resources.catalog_metric_publications
import hazlosano.app.shared.generated.resources.catalog_metric_services
import hazlosano.app.shared.generated.resources.catalog_refresh
import hazlosano.app.shared.generated.resources.catalog_search_placeholder
import hazlosano.app.shared.generated.resources.catalog_section_nearby
import hazlosano.app.shared.generated.resources.catalog_section_services
import hazlosano.app.shared.generated.resources.catalog_section_upcoming_events
import hazlosano.app.shared.generated.resources.catalog_stale_notice
import hazlosano.app.shared.generated.resources.catalog_unavailable_message
import hazlosano.app.shared.generated.resources.catalog_unavailable_title
import hazlosano.app.shared.generated.resources.pillar_info_open
import hazlosano.app.shared.generated.resources.publication_duration_minutes
import hazlosano.app.shared.generated.resources.publication_price_free
import hazlosano.app.shared.generated.resources.section_products_title
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
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PillarCatalogViewModel = rememberPillarCatalogViewModel(pillar),
    highlightsViewModel: PillarHighlightsViewModel = rememberPillarHighlightsViewModel(pillar),
) {
    val state by viewModel.uiState.collectAsState()
    val highlights by highlightsViewModel.uiState.collectAsState()

    PillarCatalogContent(
        pillar = pillar,
        state = state,
        highlights = highlights,
        onRetry = viewModel::refresh,
        onOpenInfo = onOpenInfo,
        modifier = modifier,
    )
}

/**
 * El tablero sin ViewModel, para poder componerlo en un test con un estado cualquiera.
 *
 * Un único `LazyColumn` en el que **cada sección responde de su propia espera**: la identidad del
 * pilar arriba, la comunidad debajo —campeones y retos— y el catálogo al final, con la búsqueda
 * cerrando la pantalla. Lo que no ha llegado enseña su hueco; lo que ya está se enseña.
 *
 * Antes había aquí un `when` que elegía entre una ruedita a pantalla completa y el tablero entero.
 * Esa forma de esperar apagaba también lo que no dependía de la red: con mala señal, los campeones
 * de la semana —que estaban en memoria— se escondían detrás del catálogo.
 */
@Composable
fun PillarCatalogContent(
    pillar: PillarType,
    state: PillarCatalogUiState,
    highlights: PillarHighlightsUiState,
    onRetry: () -> Unit,
    /** Por defecto no lleva a ninguna parte para poder componer el tablero suelto en un test. */
    onOpenInfo: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accent = pillar.toColor()

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = HazloSpaces.default, bottom = HazloSpaces.xl),
    ) {
        // El aviso va encima de todo: enterarse de que el tablero es viejo después de leerlo no
        // sirve de nada.
        if (state is PillarCatalogUiState.Ready && state.fromCache) {
            item { CatalogStaleNotice() }
            item { Spacer(modifier = Modifier.height(HazloSpaces.sm)) }
        }

        pillarSummary(
            pillar = pillar,
            state = state,
            onRefresh = onRetry,
            onOpenInfo = onOpenInfo,
        )

        pillarHighlights(state = highlights, accent = accent)

        pillarCatalogPlaceholder(state = state, onRetry = onRetry)

        if (state is PillarCatalogUiState.Ready) {
            pillarCatalogSections(pillar = pillar, sections = state.sections)
        }
    }
}

/**
 * El resumen del pilar: sus cifras cuando están, su hueco mientras se leen.
 *
 * Sin catálogo no hay tarjeta. Las tres cifras **son** el catálogo, y una tarjeta con tres ceros
 * diría que este pilar está vacío, que es distinto de que todavía no se haya podido leer.
 */
private fun LazyListScope.pillarSummary(
    pillar: PillarType,
    state: PillarCatalogUiState,
    onRefresh: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    when (state) {
        PillarCatalogUiState.Loading -> pillarSummarySkeleton()

        is PillarCatalogUiState.Ready -> item {
            PillarSummaryCard(
                title = pillarLabel(pillar),
                icon = pillarIcon(pillar),
                accentColor = pillar.toColor(),
                metrics = listOf(
                    PillarMetric(
                        value = state.sections.total.toString(),
                        label = stringResource(Res.string.catalog_metric_publications),
                    ),
                    PillarMetric(
                        value = state.sections.eventCount.toString(),
                        label = stringResource(Res.string.catalog_metric_events),
                    ),
                    PillarMetric(
                        value = state.sections.serviceCount.toString(),
                        label = stringResource(Res.string.catalog_metric_services),
                    ),
                ),
                refreshContentDescription = stringResource(Res.string.catalog_refresh),
                onRefresh = onRefresh,
                infoContentDescription = stringResource(Res.string.pillar_info_open),
                onInfo = onOpenInfo,
                modifier = Modifier
                    .padding(horizontal = HazloSpaces.gutter)
                    .testTag(PillarCatalogTags.SUMMARY),
            )
        }

        PillarCatalogUiState.Unavailable, PillarCatalogUiState.Failed -> Unit
    }
}

/**
 * Los carruseles y la rejilla de un pilar, para insertarlos en cualquier `LazyColumn`.
 *
 * Está aparte del tablero porque la pantalla de sueño los pinta debajo de su propio panel de
 * análisis, que es una funcionalidad real y no un hueco. Extraerlos fue mover, no copiar: el
 * tablero los sigue usando desde aquí.
 *
 * No incluye la tarjeta de resumen: quien ya tiene un encabezado propio no quiere un segundo héroe
 * a mitad de la pantalla.
 */
fun LazyListScope.pillarCatalogSections(
    pillar: PillarType,
    sections: CatalogSections,
) {
    val accent = pillar.toColor()

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
            title = stringResource(Res.string.section_products_title),
            placeholderText = stringResource(
                Res.string.catalog_search_placeholder,
                pillarLabel(pillar),
            ),
            emptyText = stringResource(Res.string.catalog_empty),
            accentColor = accent,
        )
    }
}

/**
 * Qué enseñar del catálogo mientras no está, dentro de una pantalla que sigue teniendo contenido
 * propio alrededor.
 *
 * Lo usan las dos pantallas que pintan un catálogo de pilar, y por eso el hueco es el mismo en las
 * dos: un catálogo que tarda nunca deja en blanco ni el tablero ni el panel de sueño.
 */
fun LazyListScope.pillarCatalogPlaceholder(
    state: PillarCatalogUiState,
    onRetry: () -> Unit,
) {
    when (state) {
        is PillarCatalogUiState.Ready -> Unit

        PillarCatalogUiState.Loading -> pillarCatalogSkeleton()

        PillarCatalogUiState.Unavailable -> item {
            CatalogMessage(
                title = stringResource(Res.string.catalog_unavailable_title),
                message = stringResource(Res.string.catalog_unavailable_message),
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth().testTag(PillarCatalogTags.UNAVAILABLE),
            )
        }

        PillarCatalogUiState.Failed -> item {
            CatalogMessage(
                title = stringResource(Res.string.catalog_failed_title),
                message = stringResource(Res.string.catalog_failed_message),
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Un carrusel horizontal de publicaciones, con el mismo ritmo que las secciones de sueño:
 * encabezado con margen de gutter, y la fila sangrando hasta el borde.
 */
private fun LazyListScope.publicationCarousel(
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

/**
 * Lo viejo se enseña, pero se enseña **diciendo que es viejo**.
 *
 * Sin este aviso, un precio de hace una semana y uno de hace un segundo se ven igual, que es la
 * forma silenciosa de mentir que este pilar del roadmap vino a quitar.
 */
@Composable
fun CatalogStaleNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(PillarCatalogTags.STALE_NOTICE),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = stringResource(Res.string.catalog_stale_notice),
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
            Text(stringResource(Res.string.action_retry))
        }
    }
}

private val CAROUSEL_CARD_WIDTH = 170.dp
