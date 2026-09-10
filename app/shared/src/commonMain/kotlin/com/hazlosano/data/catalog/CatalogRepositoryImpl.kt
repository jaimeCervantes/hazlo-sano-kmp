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

    override suspend fun getCatalog(
        pillar: PillarType?,
        page: Int,
        pageSize: Int,
        location: VisitorLocation?,
    ): CatalogPage {
        val remote = runCatching {
            api.fetchPage(pillar, page, pageSize, location)
        }.getOrNull() ?: return cachedOrUnavailable(pillar, pageSize, page)

        // Sin filtro las publicaciones no afirman pilar: el sitio no dice a cual pertenece cada una
        // —lo deriva de su categoria raiz, con una taxonomia que este app no tiene— y deducirlo aqui
        // seria una segunda copia de esa tabla, desincronizada desde el dia uno.
        val publications = CatalogMapper.toDomain(remote, pillar)

        // Solo la primera página sustituye la caché. Guardar la página 3 borrando las anteriores
        // dejaría al dispositivo con un trozo suelto del catálogo y sin su principio.
        //
        // **Y solo cuando hay filtro.** `replacePillar` borra lo de un pilar antes de escribir; sin
        // pilar no hay nada acotado que sustituir, y hacerlo sobre todo dejaria al dispositivo con
        // veinte publicaciones sueltas donde tenia el catalogo de los cuatro. El feed sin filtro lee
        // de lo que las pestanas de pilar hayan guardado y no escribe nada.
        if (page == 1 && pillar != null) {
            runCatching { cache.replacePillar(pillar, publications) }
        }

        return CatalogPage.Fresh(
            publications = publications,
            hasMore = remote.nextPage != null,
        )
    }

    /**
     * Buscar no cae a lo guardado, y esa es la diferencia con [getCatalog].
     *
     * La cache del app guarda publicaciones, no busquedas: no hay forma de saber si lo guardado
     * contesta a «miel» sin volver a filtrar por texto en el dispositivo, que es una segunda
     * implementacion del buscador — con otras reglas que las del sitio, y peores. Sin red, una
     * busqueda no tiene respuesta: [CatalogPage.Unavailable] lo dice.
     */
    override suspend fun searchCatalog(
        query: String,
        pillar: PillarType?,
        page: Int,
        pageSize: Int,
        location: VisitorLocation?,
    ): CatalogPage {
        if (query.isBlank()) return CatalogPage.Fresh(publications = emptyList(), hasMore = false)

        val remote = runCatching {
            api.search(query, pillar, page, pageSize, location)
        }.getOrNull() ?: return CatalogPage.Unavailable

        return CatalogPage.Fresh(
            publications = SearchMapper.toDomain(remote, CatalogApi.LOCALE, pillar),
            // El buscador no dice `nextPage`: si quedan mas es una cuenta sobre el total.
            hasMore = page * pageSize < remote.total,
        )
    }

    private suspend fun cachedOrUnavailable(
        pillar: PillarType?,
        pageSize: Int,
        page: Int,
    ): CatalogPage {
        val offset = (page - 1).coerceAtLeast(0) * pageSize
        val cached = runCatching {
            if (pillar == null) {
                cache.getAll(limit = pageSize, offset = offset)
            } else {
                cache.getByPillar(pillar, limit = pageSize, offset = offset)
            }
        }.getOrNull().orEmpty()

        // Vacío no basta para decir «nunca se leyó»: un pilar puede estar de verdad vacío en el
        // sitio, y la última página de uno lleno también vuelve vacía. Lo que lo decide es si hay
        // algo guardado, no si esta página trajo filas.
        val stored = runCatching {
            if (pillar == null) cache.count() else cache.countByPillar(pillar)
        }.getOrDefault(0L)
        if (cached.isEmpty() && stored == 0L) {
            return CatalogPage.Unavailable
        }

        return CatalogPage.Cached(cached)
    }
}
