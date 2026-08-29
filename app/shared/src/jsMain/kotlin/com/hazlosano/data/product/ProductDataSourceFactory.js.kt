package com.hazlosano.data.product

import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.HazloSeller
import com.hazlosano.domain.model.PillarType

actual fun createProductDataSource(): ProductDataSource = object : ProductDataSource {
    private val products = mutableListOf<HazloProduct>()

    override suspend fun searchProducts(query: String, limit: Int, offset: Int): List<HazloProduct> =
        products.filter { p ->
            query.isBlank() || p.name.contains(query, ignoreCase = true) ||
                p.description.contains(query, ignoreCase = true) ||
                p.category.contains(query, ignoreCase = true)
        }.drop(offset).take(limit)

    override suspend fun getByCategory(category: String, limit: Int, offset: Int): List<HazloProduct> =
        products.filter { it.category == category }.drop(offset).take(limit)

    override suspend fun getAll(limit: Int, offset: Int): List<HazloProduct> =
        products.drop(offset).take(limit)

    override suspend fun getById(id: String): HazloProduct? =
        products.firstOrNull { it.id == id }

    override suspend fun getByPillar(pillar: PillarType, limit: Int, offset: Int): List<HazloProduct> =
        products.filter { it.pillar == pillar }.drop(offset).take(limit)

    override suspend fun countByPillar(pillar: PillarType): Long =
        products.count { it.pillar == pillar }.toLong()

    override suspend fun replacePillar(pillar: PillarType, publications: List<HazloProduct>) {
        products.removeAll { it.pillar == pillar }
        products.addAll(publications)
    }

    override suspend fun saveProducts(products: List<HazloProduct>) {
        this.products.addAll(products)
    }

    override suspend fun saveSellers(sellers: List<HazloSeller>) {
        // in-memory implementation: sellers are not persisted separately
    }

    override suspend fun count(): Long = products.size.toLong()

    override suspend fun deleteAll() {
        products.clear()
    }
}
