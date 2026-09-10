package com.hazlosano.core.ui.components.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.hazlosano.core.presentation.PublicationListUiState
import com.hazlosano.core.ui.components.cards.HazloPublicationCard
import com.hazlosano.domain.model.HazloProduct
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.publication_list_empty
import kotlin.test.Test

/**
 * El hueco de las publicaciones tiene la forma de las publicaciones.
 *
 * Spec: `features/el_inicio_no_brinca.feature`, slice 2.
 *
 * El hueco anterior dibujaba dos tarjetas de ancho completo de 320 dp mientras el feed llevaba meses
 * yendo de dos en dos. **Un hueco con otra forma que la lista que espera no quita el brinco: lo
 * parte en dos.** Estas pruebas fijan el alto midiendo la tarjeta de verdad.
 */
@OptIn(ExperimentalTestApi::class)
class PublicationListSkeletonTest {

    /** Media pantalla de teléfono: es el ancho que tiene una tarjeta en una rejilla de dos. */
    private val columnWidth = 180.dp

    private val publication = HazloProduct(
        id = "miel-1",
        name = "Miel de abeja melipona",
        description = "De una meliponario familiar en Yucatán, cosechada a mano y sin calentar.",
        price = 180.0,
        imageUrl = "",
        category = "alimentacion",
    )

    /**
     * **Esta prueba es la que fija** `PublicationListDefaults.cardHeight`. No se eligió a ojo: se
     * compuso la tarjeta con la descripción a dos renglones —su caso más alto, porque el título y la
     * descripción tienen `maxLines`— y se leyó lo que medía.
     */
    @Test
    fun `the gap is as tall as the card it waits for`() = runComposeUiTest {
        setContent {
            Box(modifier = Modifier.width(columnWidth).testTag("tarjeta")) {
                HazloPublicationCard(publication = publication, onFavoriteClick = {})
            }
        }

        onNodeWithTag("tarjeta").assertHeightIsEqualTo(PublicationListDefaults.cardHeight)
    }

    @Test
    fun `a list with nothing yet and something coming shows the gap`() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                publicationList(
                    state = PublicationListUiState(loading = true),
                    emptyText = Res.string.publication_list_empty,
                    onReachedEnd = {},
                    onRetry = {},
                )
            }
        }

        onNodeWithTag(PublicationListTags.SKELETON).assertIsDisplayed()
        onNodeWithTag(PublicationListTags.LOADING_MORE).assertDoesNotExist()
    }

    /**
     * Con publicaciones ya leídas, lo que dice que viene más es la ruedita del pie.
     *
     * Tapar lo leído con un hueco sería esconder lo que ya se podía leer para anunciar lo que
     * todavía no está — exactamente al revés.
     */
    @Test
    fun `a list that already has publications shows the spinner and not the gap`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    publicationList(
                        state = PublicationListUiState(
                            publications = listOf(publication),
                            loading = true,
                            firstPagePending = false,
                        ),
                        emptyText = Res.string.publication_list_empty,
                        onReachedEnd = {},
                        onRetry = {},
                    )
                }
            }

            onNodeWithTag(PublicationListTags.LOADING_MORE).assertIsDisplayed()
            onNodeWithTag(PublicationListTags.SKELETON).assertDoesNotExist()
        }

    /** Y si no viene nada, tampoco hay hueco: lo que hay es la respuesta de que está vacío. */
    @Test
    fun `a list with nothing and nothing coming says so instead of showing a gap`() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    publicationList(
                        state = PublicationListUiState(loading = false, firstPagePending = false),
                        emptyText = Res.string.publication_list_empty,
                        onReachedEnd = {},
                        onRetry = {},
                    )
                }
            }

            onNodeWithTag(PublicationListTags.EMPTY).assertIsDisplayed()
            onNodeWithTag(PublicationListTags.SKELETON).assertDoesNotExist()
        }
}
