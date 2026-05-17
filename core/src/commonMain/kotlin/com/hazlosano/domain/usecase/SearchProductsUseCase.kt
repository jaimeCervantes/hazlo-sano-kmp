package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.repository.ProductRepository

class SearchProductsUseCase(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(query: String, category: String? = null): List<HazloProduct> =
        repository.searchProducts(query, category)
}
