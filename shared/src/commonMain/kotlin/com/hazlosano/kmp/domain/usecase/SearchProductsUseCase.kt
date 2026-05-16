package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.HazloProduct
import com.hazlosano.kmp.domain.repository.ProductRepository

class SearchProductsUseCase(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(query: String, category: String? = null): List<HazloProduct> =
        repository.searchProducts(query, category)
}
