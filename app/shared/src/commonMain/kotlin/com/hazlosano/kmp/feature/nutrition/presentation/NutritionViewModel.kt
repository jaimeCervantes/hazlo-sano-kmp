package com.hazlosano.kmp.feature.nutrition.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.kmp.domain.model.HazloProduct
import com.hazlosano.kmp.domain.usecase.SearchProductsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NutritionUiState(
    val products: List<HazloProduct> = emptyList(),
    val allProducts: List<HazloProduct> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
)

class NutritionViewModel(
    private val searchProductsUseCase: SearchProductsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            val results = if (query.isBlank()) {
                _uiState.value.allProducts
            } else {
                searchProductsUseCase(query)
            }
            _uiState.update { it.copy(products = results) }
        }
    }

    fun refresh() {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val products = searchProductsUseCase("")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        products = products,
                        allProducts = products,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error al cargar productos")
                }
            }
        }
    }
}
