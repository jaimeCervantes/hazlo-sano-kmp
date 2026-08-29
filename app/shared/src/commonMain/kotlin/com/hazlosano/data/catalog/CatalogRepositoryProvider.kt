package com.hazlosano.data.catalog

import com.hazlosano.data.product.ProductDataSource
import com.hazlosano.data.product.createProductDataSource
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.HazloSeller
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.CatalogRepository

/**
 * El catálogo, con caché si el dispositivo tiene base y sin ella si no.
 *
 * Un target sin base **no se queda sin catálogo**: pierde el poder abrirlo sin red, que es otra
 * cosa. Fallar aquí dejaría la pantalla vacía en escritorio y web por una carencia que solo importa
 * cuando no hay cobertura.
 */
fun catalogRepository(
    baseUrl: String = CatalogApi.DEFAULT_BASE_URL,
): CatalogRepository = CatalogRepositoryImpl(
    api = CatalogApi(CatalogApi.defaultClient(), baseUrl),
    cache = runCatching { createProductDataSource() }.getOrElse { NoOpProductCache },
)

/** Guarda en el vacío y devuelve vacío: el catálogo remoto sigue funcionando encima de esto. */
private object NoOpProductCache : ProductDataSource {
    override suspend fun searchProducts(query: String, limit: Int, offset: Int): List<HazloProduct> = emptyList()
    override suspend fun getByCategory(category: String, limit: Int, offset: Int): List<HazloProduct> = emptyList()
    override suspend fun getAll(limit: Int, offset: Int): List<HazloProduct> = emptyList()
    override suspend fun getById(id: String): HazloProduct? = null
    override suspend fun getByPillar(pillar: PillarType, limit: Int, offset: Int): List<HazloProduct> = emptyList()
    override suspend fun countByPillar(pillar: PillarType): Long = 0L
    override suspend fun replacePillar(pillar: PillarType, publications: List<HazloProduct>) = Unit
    override suspend fun saveProducts(products: List<HazloProduct>) = Unit
    override suspend fun saveSellers(sellers: List<HazloSeller>) = Unit
    override suspend fun count(): Long = 0L
    override suspend fun deleteAll() = Unit
}
