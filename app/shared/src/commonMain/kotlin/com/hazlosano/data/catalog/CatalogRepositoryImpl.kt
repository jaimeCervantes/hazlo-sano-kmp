package com.hazlosano.data.catalog

import com.hazlosano.data.product.ProductDataSource
import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.repository.CatalogRepository

/**
 * El sitio primero; lo guardado, solo cuando el sitio no contesta.
 *
 * El orden importa y es al revés que en una caché de rendimiento: aquí lo local no se consulta para
 * ahorrar una petición, sino para tener algo que enseñar cuando no hay red. Leer la caché primero
 * dejaría ver precios viejos teniendo cobertura, que es justo lo que este slice vino a quitar.
 */
internal class CatalogRepositoryImpl(
    private val api: CatalogApi,
    private val cache: ProductDataSource,
) : CatalogRepository {

    override suspend fun getPillarCatalog(
        pillar: PillarType,
        page: Int,
        pageSize: Int,
        location: VisitorLocation?,
    ): CatalogPage {
        val remote = runCatching {
            api.fetchPillarPage(pillar, page, pageSize, location)
        }.getOrNull() ?: return cachedOrUnavailable(pillar, pageSize, page)

        val publications = CatalogMapper.toDomain(remote, pillar)

        // Solo la primera página sustituye la caché. Guardar la página 3 borrando las anteriores
        // dejaría al dispositivo con un trozo suelto del catálogo y sin su principio.
        if (page == 1) {
            runCatching { cache.replacePillar(pillar, publications) }
        }

        return CatalogPage.Fresh(
            publications = publications,
            hasMore = remote.nextPage != null,
        )
    }

    private suspend fun cachedOrUnavailable(
        pillar: PillarType,
        pageSize: Int,
        page: Int,
    ): CatalogPage {
        val cached = runCatching {
            cache.getByPillar(pillar, limit = pageSize, offset = (page - 1).coerceAtLeast(0) * pageSize)
        }.getOrNull().orEmpty()

        // Vacío no basta para decir «nunca se leyó»: un pilar puede estar de verdad vacío en el
        // sitio, y la última página de uno lleno también vuelve vacía. Lo que lo decide es si hay
        // algo guardado de ese pilar, no si esta página trajo filas.
        if (cached.isEmpty() && runCatching { cache.countByPillar(pillar) }.getOrDefault(0L) == 0L) {
            return CatalogPage.Unavailable
        }

        return CatalogPage.Cached(cached)
    }
}
