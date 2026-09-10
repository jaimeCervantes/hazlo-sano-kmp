package com.hazlosano.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.hazlosano.core.presentation.PublicationListUiState
import com.hazlosano.core.ui.components.sections.PillarFilterChips
import com.hazlosano.core.ui.components.sections.publicationList
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.model.PillarType
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.search_no_results

/**
 * Etiquetas de prueba: la búsqueda se afirma por aquí y no por su redacción.
 *
 * [FIELD] es de la barra de arriba y no de esta pantalla, pero vive aquí porque **es la misma
 * funcionalidad**: quien busque la etiqueta del campo la busca junto a la de sus resultados.
 */
object SearchTags {
    const val FIELD: String = "search_field"
    const val RESULTS: String = "search_results"
}

/**
 * Lo que el buscador del sitio contesta, en su propia pantalla.
 *
 * **No lleva barra de abajo ni pestaña marcada.** Los resultados no son de ninguna pestaña: entran a
 * los cuatro pilares porque se busca desde arriba, y dejar «Nutrición» encendida mientras se leen
 * resultados de los cuatro diría algo falso. Es lo que ya hace el historial de sueño, que también
 * ocupa el cuerpo entero sin tocar la pestaña elegida — y por eso al volver se reaparece donde se
 * estaba, sin que nadie tenga que restaurar nada.
 *
 * El campo donde se escribe **no está aquí**: vive en la barra de arriba, que se queda quieta
 * mientras el cuerpo cambia. Esta pantalla sólo lee resultados.
 */
@Composable
fun SearchResultsScreen(
    results: PublicationListUiState,
    onSelectFilter: (PillarType?) -> Unit,
    onReachedEnd: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(SearchTags.RESULTS),
        contentPadding = PaddingValues(top = HazloSpaces.default, bottom = HazloSpaces.xl),
    ) {
        // Los chips acotan **los resultados**, no el feed: son el mismo gesto sobre otra lista.
        item {
            PillarFilterChips(selected = results.filter, onSelect = onSelectFilter)
            Spacer(modifier = Modifier.height(HazloSpaces.sm))
        }

        publicationList(
            state = results,
            emptyText = Res.string.search_no_results,
            onReachedEnd = onReachedEnd,
            onRetry = onRetry,
        )
    }
}
