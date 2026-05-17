package com.hazlosano.feature.nutrition.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.components.sections.HazloExploreProductsSection
import com.hazlosano.core.ui.theme.PillarNutrition
import com.hazlosano.feature.nutrition.presentation.NutritionViewModel

@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PillarNutrition,
                )
            }
            state.error != null -> {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            }
            else -> {
                HazloExploreProductsSection(
                    products = state.products,
                    modifier = Modifier.fillMaxSize(),
                    title = "Nutrición",
                    placeholderText = "Buscar productos...",
                    emptyText = "No se encontraron productos",
                    accentColor = PillarNutrition,
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    isLoading = state.isLoading,
                    error = state.error,
                )
            }
        }
    }
}
