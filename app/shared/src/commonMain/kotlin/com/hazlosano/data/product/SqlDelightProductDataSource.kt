package com.hazlosano.data.product

import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.HazloSeller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SqlDelightProductDataSource(
    private val database: HazloSanoDatabase,
) : ProductDataSource {

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

    override suspend fun deleteAll(): Unit =
        withContext(Dispatchers.Default) {
            // The generated mutator returns how many rows it touched; the contract here is "they
            // are gone", so the count is dropped rather than widened into the interface.
            productQueries.deleteAll()
            Unit
        }

    private fun com.hazlosano.data.db.ProductEntity.toDomain(): HazloProduct = HazloProduct(
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
