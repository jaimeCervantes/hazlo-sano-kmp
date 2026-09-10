package com.hazlosano.core.ui.components.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.hazlosano.core.presentation.PublicationListUiState
import com.hazlosano.core.ui.components.cards.HazloPublicationCard
import com.hazlosano.core.ui.theme.HazloSpaces
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.action_retry
import hazlosano.app.shared.generated.resources.publication_list_failed
import hazlosano.app.shared.generated.resources.publication_list_no_more
import hazlosano.app.shared.generated.resources.publication_list_stale
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: la lista se afirma por aquí y no por su redacción. */
object PublicationListTags {
    const val EMPTY: String = "publication_list_empty"
    const val FAILED: String = "publication_list_failed"
    const val STALE: String = "publication_list_stale"
    const val LOADING_MORE: String = "publication_list_loading_more"

    fun publication(id: String): String = "publication_$id"
}

/**
 * Las publicaciones de una lista paginada y lo que hay que decir al final de ellas.
 *
 * Se añade a una `LazyColumn` que ya existe en vez de traer la suya: dos listas perezosas anidadas
 * en el mismo eje no se pueden medir, y las dos pantallas que la usan ya son una.
 *
 * **[onReachedEnd] se llama al pintar la última fila**, que es el equivalente en Compose del
 * `IntersectionObserver` del sitio: no hay que preguntar por la posición del scroll, basta con que
 * el último elemento entre en composición. Que se llame de más no importa —el paginador ignora la
 * petición si ya hay una en vuelo— y es lo que hace que la guarda tenga que existir.
 *
 * [emptyText] entra por parámetro porque **vacío no significa lo mismo en las dos pantallas**: en el
 * feed es «aquí todavía no hay nada publicado» y en la búsqueda es «no se encontró nada con ese
 * término». La lista no sabe cuál de las dos es, y adivinarlo por la forma del estado sería inventar
 * una diferencia que quien llama ya conoce.
 */
fun LazyListScope.publicationList(
    state: PublicationListUiState,
    emptyText: StringResource,
    onReachedEnd: () -> Unit,
    onRetry: () -> Unit,
) {
    if (state.stale) {
        item {
            ListNote(
                text = stringResource(Res.string.publication_list_stale),
                tag = PublicationListTags.STALE,
            )
        }
    }

    // Dos por fila, como las rejillas de los pilares, y con su misma funcion de repartir: en un
    // telefono en vertical una tarjeta a lo ancho por publicacion convierte la lista en un pasillo.
    //
    // Se reparte en filas y cada fila es un elemento de la lista perezosa, en vez de una rejilla
    // perezosa dentro de otra lista perezosa — dos de esas en el mismo eje no se pueden medir.
    val rows = chunkExploreSectionItems(state.publications, gridColumns = LIST_COLUMNS)

    itemsIndexed(rows) { index, rowItems ->
        if (index == rows.lastIndex) onReachedEnd()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.xs),
            horizontalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
        ) {
            rowItems.forEach { publication ->
                HazloPublicationCard(
                    publication = publication,
                    onFavoriteClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .testTag(PublicationListTags.publication(publication.id)),
                )
            }
            // La ultima fila puede venir a medias: el hueco se reserva para que la tarjeta que si
            // esta no crezca al doble de las de arriba.
            repeat(LIST_COLUMNS - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    item {
        ListFooter(state = state, emptyText = emptyText, onRetry = onRetry)
    }
}

/** Dos, como las rejillas de los pilares. */
private const val LIST_COLUMNS = 2

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
private fun ListFooter(
    state: PublicationListUiState,
    emptyText: StringResource,
    onRetry: () -> Unit,
) {
    when {
        state.failed -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HazloSpaces.gutter)
                .testTag(PublicationListTags.FAILED),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.publication_list_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(Res.string.action_retry))
            }
        }

        state.loading -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HazloSpaces.gutter)
                .testTag(PublicationListTags.LOADING_MORE),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        // Vacío de verdad, no «todavía no ha llegado»: son dos cosas y sólo una es una respuesta.
        state.publications.isEmpty() && !state.firstPagePending -> ListNote(
            text = stringResource(emptyText),
            tag = PublicationListTags.EMPTY,
        )

        // «No hay más» sólo se le dice a quien llegó hasta el final habiendo leído algo.
        state.publications.isNotEmpty() && !state.hasMore -> ListNote(
            text = stringResource(Res.string.publication_list_no_more),
            tag = null,
        )
    }
}

@Composable
private fun ListNote(text: String, tag: String?) {
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
