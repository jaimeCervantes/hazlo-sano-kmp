package com.hazlosano.feature.home.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.hazlosano.core.ui.components.cards.HazloPublicationCard
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.model.pillarShortLabel
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.model.PillarType
import com.hazlosano.feature.home.presentation.HomeFeedUiState
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.home_feed_empty
import hazlosano.app.shared.generated.resources.home_feed_failed
import hazlosano.app.shared.generated.resources.home_feed_filter_all
import hazlosano.app.shared.generated.resources.home_feed_no_more
import hazlosano.app.shared.generated.resources.home_feed_retry
import hazlosano.app.shared.generated.resources.home_feed_stale
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: el feed se afirma por aquí y no por su redacción. */
object HomeFeedTags {
    const val FILTERS: String = "home_feed_filters"
    const val EMPTY: String = "home_feed_empty"
    const val FAILED: String = "home_feed_failed"
    const val STALE: String = "home_feed_stale"
    const val LOADING_MORE: String = "home_feed_loading_more"

    fun filter(pillar: PillarType?): String = "home_feed_filter_${pillar?.key ?: "all"}"

    fun publication(id: String): String = "home_feed_publication_$id"
}

/**
 * Los chips que eligen pilar, más «Todos».
 *
 * «Todos» no es un pilar más disfrazado: es el filtro ausente, y por eso su valor es `null` — lo
 * mismo que el sitio entiende por no mandar el parámetro. Un `PillarType.ALL` inventado habría
 * obligado a cada sitio que recorre los pilares a acordarse de saltárselo.
 */
@Composable
fun HomeFeedFilters(
    selected: PillarType?,
    onSelect: (PillarType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = HazloSpaces.gutter)
            .testTag(HomeFeedTags.FILTERS),
        horizontalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
    ) {
        FeedFilterChip(
            pillar = null,
            label = stringResource(Res.string.home_feed_filter_all),
            selected = selected == null,
            onSelect = onSelect,
        )
        PillarType.entries.forEach { pillar ->
            FeedFilterChip(
                pillar = pillar,
                label = pillarShortLabel(pillar),
                selected = selected == pillar,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun FeedFilterChip(
    pillar: PillarType?,
    label: String,
    selected: Boolean,
    onSelect: (PillarType?) -> Unit,
) {
    // Cada pilar se pinta con el suyo; «Todos» con el color primario, porque no es de ninguno.
    val accent = pillar?.palette()?.solid ?: MaterialTheme.colorScheme.primary

    FilterChip(
        selected = selected,
        onClick = { onSelect(pillar) },
        label = { Text(label) },
        modifier = Modifier.testTag(HomeFeedTags.filter(pillar)),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent),
    )
}

/**
 * Las publicaciones del feed y lo que hay que decir al final de ellas.
 *
 * Se añade a una `LazyColumn` que ya existe en vez de traer la suya: dos listas perezosas anidadas
 * en el mismo eje no se pueden medir, y el Inicio ya es una.
 *
 * **[onReachedEnd] se llama al pintar la última publicación**, que es el equivalente en Compose del
 * `IntersectionObserver` del sitio: no hay que preguntar por la posición del scroll, basta con que
 * el último elemento entre en composición. Que se llame de más no importa —el ViewModel ignora la
 * petición si ya hay una en vuelo— y es lo que hace que la guarda tenga que existir.
 */
fun LazyListScope.homeFeed(
    state: HomeFeedUiState,
    onReachedEnd: () -> Unit,
    onRetry: () -> Unit,
) {
    if (state.stale) {
        item {
            FeedNote(
                text = stringResource(Res.string.home_feed_stale),
                tag = HomeFeedTags.STALE,
            )
        }
    }

    itemsIndexed(state.publications) { index, publication ->
        if (index == state.publications.lastIndex) onReachedEnd()
        HazloPublicationCard(
            publication = publication,
            onFavoriteClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.xs)
                .testTag(HomeFeedTags.publication(publication.id)),
        )
    }

    item {
        FeedFooter(state = state, onRetry = onRetry)
    }
}

/** `itemsIndexed` con la firma que hace falta, sin arrastrar la de claves que aquí no se usa. */
private fun <T> LazyListScope.itemsIndexed(
    items: List<T>,
    itemContent: @Composable (Int, T) -> Unit,
) = items(count = items.size) { index -> itemContent(index, items[index]) }

/**
 * Qué se dice debajo de lo cargado.
 *
 * Las cuatro respuestas son excluyentes y su orden es el de la urgencia: si algo falló hay que
 * decirlo aunque además esté cargando, y «no hay más» sólo tiene sentido cuando hay algo.
 */
@Composable
private fun FeedFooter(state: HomeFeedUiState, onRetry: () -> Unit) {
    when {
        state.failed -> Column(
            modifier = Modifier.fillMaxWidth().padding(HazloSpaces.gutter).testTag(HomeFeedTags.FAILED),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.home_feed_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(Res.string.home_feed_retry))
            }
        }

        state.loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HazloSpaces.gutter)
                .testTag(HomeFeedTags.LOADING_MORE),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        // Vacío de verdad, no «todavía no ha llegado»: son dos cosas y sólo una es una respuesta.
        state.publications.isEmpty() && !state.firstPagePending -> FeedNote(
            text = stringResource(Res.string.home_feed_empty),
            tag = HomeFeedTags.EMPTY,
        )

        // «No hay más» sólo se le dice a quien llegó hasta el final habiendo leído algo.
        state.publications.isNotEmpty() -> FeedNote(
            text = stringResource(Res.string.home_feed_no_more),
            tag = null,
        )
    }
}

@Composable
private fun FeedNote(text: String, tag: String?) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(HazloSpaces.gutter)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
    )
}
