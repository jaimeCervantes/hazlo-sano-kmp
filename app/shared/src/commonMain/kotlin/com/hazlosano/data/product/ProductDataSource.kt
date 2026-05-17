package com.hazlosano.data.product

import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.HazloSeller

interface ProductDataSource {
    suspend fun searchProducts(query: String, limit: Int = 50, offset: Int = 0): List<HazloProduct>
    suspend fun getByCategory(category: String, limit: Int = 50, offset: Int = 0): List<HazloProduct>
    suspend fun getAll(limit: Int = 50, offset: Int = 0): List<HazloProduct>
    suspend fun getById(id: String): HazloProduct?
    suspend fun saveProducts(products: List<HazloProduct>)
    suspend fun saveSellers(sellers: List<HazloSeller>)
    suspend fun count(): Long
    suspend fun deleteAll()
}

expect fun createProductDataSource(): ProductDataSource
