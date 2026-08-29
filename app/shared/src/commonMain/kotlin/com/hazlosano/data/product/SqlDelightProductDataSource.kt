package com.hazlosano.data.product

import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.HazloSeller
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.PublicationKind
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

    override suspend fun getByPillar(pillar: PillarType, limit: Int, offset: Int): List<HazloProduct> =
        withContext(Dispatchers.Default) {
            productQueries.selectByPillar(pillar.key, limit.toLong(), offset.toLong())
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun countByPillar(pillar: PillarType): Long =
        withContext(Dispatchers.Default) {
            productQueries.countByPillar(pillar.key).executeAsOne()
        }

    override suspend fun replacePillar(pillar: PillarType, publications: List<HazloProduct>): Unit =
        withContext(Dispatchers.Default) {
            database.transaction {
                productQueries.deleteByPillar(pillar.key)
                publications.forEach { productQueries.insert(it) }
            }
        }

    override suspend fun saveProducts(products: List<HazloProduct>) =
        withContext(Dispatchers.Default) {
            database.transaction {
                products.forEach { productQueries.insert(it) }
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

    private fun com.hazlosano.data.db.ProductEntityQueries.insert(product: HazloProduct) {
        insertOrReplace(
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
            kind = product.kind.key,
            pillar = product.pillar?.key,
            startsAt = product.startsAtEpochMillis,
            endsAt = product.endsAtEpochMillis,
            durationMinutes = product.durationMinutes?.toLong(),
        )
    }

    private fun com.hazlosano.data.db.ProductEntity.toDomain(): HazloProduct = HazloProduct(
        id = id,
        name = name,
        description = description ?: "",
        price = price,
        imageUrl = imageUrl ?: "",
        isFavorite = false,
        // La distancia no se guarda: se midió desde donde estaba el teléfono al leer, y enseñarla
        // días después desde otra ciudad sería peor que no enseñarla.
        distanceMeters = null,
        category = category,
        subCategory = subCategory,
        tags = parseTags(tags),
        productUrl = productUrl,
        sellerId = sellerId,
        isAvailable = isAvailable != 0L,
        kind = PublicationKind.fromKey(kind),
        pillar = PillarType.fromKey(pillar),
        startsAtEpochMillis = startsAt,
        endsAtEpochMillis = endsAt,
        durationMinutes = durationMinutes?.toInt(),
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
