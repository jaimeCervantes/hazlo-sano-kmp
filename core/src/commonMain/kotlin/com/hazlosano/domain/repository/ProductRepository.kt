package com.hazlosano.domain.repository

import com.hazlosano.domain.model.HazloProduct

interface ProductRepository {
    suspend fun searchProducts(query: String, category: String? = null, limit: Int = 50, offset: Int = 0): List<HazloProduct>
    suspend fun getByCategory(category: String, limit: Int = 50, offset: Int = 0): List<HazloProduct>
    suspend fun getAll(limit: Int = 50, offset: Int = 0): List<HazloProduct>
    suspend fun getById(id: String): HazloProduct?
}
