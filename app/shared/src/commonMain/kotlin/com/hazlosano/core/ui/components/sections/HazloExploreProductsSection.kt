package com.hazlosano.core.ui.components.sections

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.hazlosano.core.ui.components.atomic.HazloAsyncImage
import com.hazlosano.core.ui.components.cards.HazloProductCard
import com.hazlosano.core.ui.components.cards.HazloProductCardImagePlaceholder
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PublicationKind
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.publication_price_free
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
) {
    // Un evento sin precio es gratis y se dice; un anuncio sin precio simplemente no se vende, y
    // ahí la línea se queda vacía en lugar de anunciar un "Gratis" que no significa nada.
    val freeLabel = stringResource(Res.string.publication_price_free)

    val productContent: @Composable (HazloProduct, Modifier) -> Unit = { product, itemModifier ->
        HazloProductCard(
            title = product.name,
            description = product.description,
            price = product.price,
            priceFallbackLabel = freeLabel.takeIf { product.kind == PublicationKind.EVENT },
            isFavorite = product.isFavorite,
            distanceMeters = product.distanceMeters,
            onFavoriteClick = { onFavoriteClick(product) },
            accentColor = accentColor,
            modifier = itemModifier,
            imageContent = {
                if (product.imageUrl.isBlank()) {
                    HazloProductCardImagePlaceholder(
                        title = product.name,
                        accentColor = accentColor,
                    )
                } else {
                    HazloAsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
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
