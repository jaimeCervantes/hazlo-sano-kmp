package com.hazlosano.core.ui.components.sections

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hazlosano.core.ui.components.cards.HazloPublicationCard
import com.hazlosano.domain.model.HazloProduct
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.section_products_empty
import hazlosano.app.shared.generated.resources.section_products_placeholder
import hazlosano.app.shared.generated.resources.section_products_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun HazloExploreProductsSection(
    products: List<HazloProduct>,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.section_products_title),
    placeholderText: String = stringResource(Res.string.section_products_placeholder),
    emptyText: String = stringResource(Res.string.section_products_empty),
    gridColumns: Int = 2,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onFavoriteClick: (HazloProduct) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    /**
     * Qué decir encima del título de una publicación: cuánto dura un servicio, cuándo ocurre un
     * evento, nada en lo demás.
     *
     * Llega como parámetro porque decidirlo es conocimiento del catálogo y esta sección sólo sabe
     * pintar una rejilla. Entró al quitar el carrusel de servicios, que era el único sitio donde esa
     * línea se veía: sin esto, la duración de un servicio habría desaparecido del app.
     */
    overlineLabel: (@Composable (HazloProduct) -> String?)? = null,
) {
    // La traduccion de una publicacion a una tarjeta vive en `HazloPublicationCard` desde que el
    // feed del Inicio pinta lo mismo: estaba escrita aqui, donde solo la alcanzaba esta rejilla.
    val productContent: @Composable (HazloProduct, Modifier) -> Unit = { product, itemModifier ->
        HazloPublicationCard(
            publication = product,
            onFavoriteClick = { onFavoriteClick(product) },
            modifier = itemModifier,
            accentColor = accentColor,
            overlineLabel = overlineLabel?.invoke(product),
        )
    }

    if (onSearchQueryChange == null) {
        HazloExploreItemsSection(
            items = products,
            matchesQuery = ::matchesHazloProductQuery,
            modifier = modifier,
            title = title,
            placeholderText = placeholderText,
            emptyText = emptyText,
            gridColumns = gridColumns,
            accentColor = accentColor,
            itemContent = productContent,
        )
    } else {
        HazloExploreItemsSection(
            items = products,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            matchesQuery = { _, _ -> true },
            modifier = modifier,
            title = title,
            placeholderText = placeholderText,
            emptyText = emptyText,
            gridColumns = gridColumns,
            accentColor = accentColor,
            itemContent = productContent,
        )
    }
}

internal fun matchesHazloProductQuery(
    product: HazloProduct,
    query: String,
): Boolean {
    if (query.isBlank()) return true
    val q = query.trim()
    return product.name.contains(q, ignoreCase = true) ||
        product.description.contains(q, ignoreCase = true) ||
        product.category.contains(q, ignoreCase = true) ||
        product.subCategory?.contains(q, ignoreCase = true) == true ||
        product.tags.any { it.contains(q, ignoreCase = true) }
}
