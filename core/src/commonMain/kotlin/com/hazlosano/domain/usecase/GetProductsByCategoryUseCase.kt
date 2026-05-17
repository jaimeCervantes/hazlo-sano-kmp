package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.repository.ProductRepository

class GetProductsByCategoryUseCase(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(category: String): List<HazloProduct> =
        repository.getByCategory(category)
}
