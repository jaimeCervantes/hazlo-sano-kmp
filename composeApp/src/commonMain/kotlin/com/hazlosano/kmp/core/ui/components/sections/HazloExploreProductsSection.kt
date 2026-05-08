package com.hazlosano.kmp.core.ui.components.sections

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.hazlosano.kmp.core.ui.components.atomic.HazloAsyncImage
import com.hazlosano.kmp.core.ui.components.cards.HazloProductCard
import com.hazlosano.kmp.core.ui.components.cards.HazloProductCardImagePlaceholder
import com.hazlosano.kmp.domain.model.HazloProduct

@Composable
fun HazloExploreProductsSection(
    products: List<HazloProduct>,
    modifier: Modifier = Modifier,
    title: String = HazloSectionDefaults.productSearchTitle,
    placeholderText: String = HazloSectionDefaults.productSearchPlaceholder,
    emptyText: String = HazloSectionDefaults.productSearchEmptyText,
    gridColumns: Int = 2,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onFavoriteClick: (HazloProduct) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    isLoading: Boolean = false,
    error: String? = null,
) {
    val productContent: @Composable (HazloProduct, Modifier) -> Unit = { product, itemModifier ->
        HazloProductCard(
            title = product.name,
            description = product.description,
            price = product.price,
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
            isLoading = isLoading,
            error = error,
            itemContent = productContent,
        )
    }
}

internal fun matchesHazloProductQuery(
    product: HazloProduct,
    query: String,
): Boolean {
    return product.name.contains(query, ignoreCase = true)
}
