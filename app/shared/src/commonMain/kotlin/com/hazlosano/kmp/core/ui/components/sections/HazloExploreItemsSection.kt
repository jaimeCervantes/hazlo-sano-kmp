package com.hazlosano.kmp.core.ui.components.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

object HazloExploreItemsSectionDefaults {
    const val title: String = "Buscar productos y servicios"
    const val placeholderText: String = "Buscar productos cercanos..."
    const val emptyText: String = "No se encontraron productos."
    const val gridColumns: Int = 2
    const val minimumGridColumns: Int = 1
    const val itemWeight: Float = 1f
}

@Composable
fun <T> HazloExploreItemsSection(
    items: List<T>,
    matchesQuery: (T, String) -> Boolean,
    modifier: Modifier = Modifier,
    title: String = HazloExploreItemsSectionDefaults.title,
    placeholderText: String = HazloExploreItemsSectionDefaults.placeholderText,
    emptyText: String = HazloExploreItemsSectionDefaults.emptyText,
    gridColumns: Int = HazloExploreItemsSectionDefaults.gridColumns,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    HazloExploreItemsSection(
        items = items,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        matchesQuery = matchesQuery,
        modifier = modifier,
        title = title,
        placeholderText = placeholderText,
        emptyText = emptyText,
        gridColumns = gridColumns,
        accentColor = accentColor,
        itemContent = itemContent,
    )
}

@Composable
fun <T> HazloExploreItemsSection(
    items: List<T>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    matchesQuery: (T, String) -> Boolean,
    modifier: Modifier = Modifier,
    title: String = HazloExploreItemsSectionDefaults.title,
    placeholderText: String = HazloExploreItemsSectionDefaults.placeholderText,
    emptyText: String = HazloExploreItemsSectionDefaults.emptyText,
    gridColumns: Int = HazloExploreItemsSectionDefaults.gridColumns,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isLoading: Boolean = false,
    error: String? = null,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    val filteredItems = remember(searchQuery, items, matchesQuery) {
        filterExploreSectionItems(
            items = items,
            query = searchQuery,
            matchesQuery = matchesQuery,
        )
    }
    val chunkedItems = remember(filteredItems, gridColumns) {
        chunkExploreSectionItems(
            items = filteredItems,
            gridColumns = gridColumns,
        )
    }
    val resolvedGridColumns = remember(gridColumns) {
        resolveExploreSectionGridColumns(gridColumns)
    }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))

        HazloExploreSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholderText = placeholderText,
            accentColor = accentColor,
        )

        Spacer(modifier = Modifier.height(16.dp))

        error?.let { errorMessage ->
            Text(
                text = errorMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            Text(
                text = "Buscando productos...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (filteredItems.isEmpty() && !isLoading) {
            Text(
                text = emptyText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                chunkedItems.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        rowItems.forEach { item ->
                            itemContent(
                                item,
                                Modifier.weight(HazloExploreItemsSectionDefaults.itemWeight),
                            )
                        }

                        repeat(resolvedGridColumns - rowItems.size) {
                            Spacer(
                                modifier = Modifier.weight(HazloExploreItemsSectionDefaults.itemWeight),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HazloExploreSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholderText) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = accentColor,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

internal fun <T> filterExploreSectionItems(
    items: List<T>,
    query: String,
    matchesQuery: (T, String) -> Boolean,
): List<T> {
    return if (query.isBlank()) {
        items
    } else {
        items.filter { item -> matchesQuery(item, query) }
    }
}

internal fun <T> chunkExploreSectionItems(
    items: List<T>,
    gridColumns: Int,
): List<List<T>> {
    val resolvedGridColumns = resolveExploreSectionGridColumns(gridColumns)
    return items.chunked(resolvedGridColumns)
}

internal fun resolveExploreSectionGridColumns(gridColumns: Int): Int {
    return gridColumns.coerceAtLeast(HazloExploreItemsSectionDefaults.minimumGridColumns)
}
