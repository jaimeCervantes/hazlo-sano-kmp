package com.hazlosano.kmp.data.product

import app.cash.sqldelight.db.SqlDriver
import com.hazlosano.kmp.data.db.HazloSanoDatabase
import com.hazlosano.kmp.domain.model.HazloProduct
import com.hazlosano.kmp.domain.model.HazloSeller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



internal class SqlDelightProductDataSource(
    driver: SqlDriver,
) : ProductDataSource {

    private val database = HazloSanoDatabase(driver)
    private val productQueries = database.productEntityQueries
    private val sellerQueries = database.sellerEntityQueries

    override suspend fun searchProducts(query: String, limit: Int, offset: Int): List<HazloProduct> =
        withContext(Dispatchers.Default) {
            val q = query.trim()
            if (q.isEmpty()) {
                productQueries.selectAll(limit.toLong(), offset.toLong()).executeAsList().map { it.toDomain() }
            } else {
                productQueries.searchByText(q, q, q, limit.toLong(), offset.toLong()).executeAsList().map { it.toDomain() }
            }
        }

    override suspend fun getByCategory(category: String, limit: Int, offset: Int): List<HazloProduct> =
        withContext(Dispatchers.Default) {
            productQueries.selectByCategory(category, limit.toLong(), offset.toLong()).executeAsList().map { it.toDomain() }
        }

    override suspend fun getAll(limit: Int, offset: Int): List<HazloProduct> =
        withContext(Dispatchers.Default) {
            productQueries.selectAll(limit.toLong(), offset.toLong()).executeAsList().map { it.toDomain() }
        }

    override suspend fun getById(id: String): HazloProduct? =
        withContext(Dispatchers.Default) {
            productQueries.selectById(id).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun saveProducts(products: List<HazloProduct>) =
        withContext(Dispatchers.Default) {
            database.transaction {
                products.forEach { product ->
                    productQueries.insertOrReplace(
                        id = product.id,
                        name = product.name,
                        price = product.price,
                        isAvailable = if (product.isAvailable) 1L else 0L,
                        description = product.description,
                        category = product.category,
                        subCategory = product.subCategory,
                        tags = product.tags.joinToString(prefix = "[", postfix = "]") { "\"$it\"" },
                        imageUrl = product.imageUrl,
                        productUrl = product.productUrl,
                        sellerId = product.sellerId,
                    )
                }
            }
        }

    override suspend fun saveSellers(sellers: List<HazloSeller>) =
        withContext(Dispatchers.Default) {
            database.transaction {
                sellers.forEach { seller ->
                    sellerQueries.insertOrReplace(
                        id = seller.id,
                        name = seller.name,
                        category = seller.category,
                        phone = seller.phone,
                        url = seller.url,
                        description = seller.description,
                        logoUrl = seller.logoUrl,
                        hasMembership = if (seller.hasMembership) 1L else 0L,
                        hasPaidAds = if (seller.hasPaidAds) 1L else 0L,
                    )
                }
            }
        }

    override suspend fun count(): Long =
        withContext(Dispatchers.Default) {
            productQueries.countAll().executeAsOne()
        }

    override suspend fun deleteAll() =
        withContext(Dispatchers.Default) {
            productQueries.deleteAll()
        }

    private fun com.hazlosano.kmp.data.db.ProductEntity.toDomain(): HazloProduct = HazloProduct(
        id = id,
        name = name,
        description = description ?: "",
        price = price,
        imageUrl = imageUrl ?: "",
        isFavorite = false,
        distanceMeters = null,
        category = category,
        subCategory = subCategory,
        tags = parseTags(tags),
        productUrl = productUrl,
        sellerId = sellerId,
        isAvailable = isAvailable != 0L,
    )

    private fun parseTags(raw: String): List<String> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return raw
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }
}
