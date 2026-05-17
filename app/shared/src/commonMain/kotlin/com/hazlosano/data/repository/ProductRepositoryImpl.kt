package com.hazlosano.data.repository

import com.hazlosano.data.product.ProductDataSource
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.repository.ProductRepository

class ProductRepositoryImpl(
    private val dataSource: ProductDataSource,
) : ProductRepository {

    override suspend fun searchProducts(query: String, category: String?, limit: Int, offset: Int): List<HazloProduct> {
        if (query.isBlank() && category != null) {
            return dataSource.getByCategory(category, limit, offset)
        }
        if (query.isBlank()) {
            return dataSource.getAll(limit, offset)
        }
        val results = dataSource.searchProducts(query, limit, offset)
        return if (category != null) results.filter { it.category == category } else results
    }

    override suspend fun getByCategory(category: String, limit: Int, offset: Int): List<HazloProduct> =
        dataSource.getByCategory(category, limit, offset)

    override suspend fun getAll(limit: Int, offset: Int): List<HazloProduct> =
        dataSource.getAll(limit, offset)

    override suspend fun getById(id: String): HazloProduct? =
        dataSource.getById(id)
}
