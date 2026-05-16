package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.HazloProduct
import com.hazlosano.kmp.domain.repository.ProductRepository

class GetProductsByCategoryUseCase(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(category: String): List<HazloProduct> =
        repository.getByCategory(category)
}
