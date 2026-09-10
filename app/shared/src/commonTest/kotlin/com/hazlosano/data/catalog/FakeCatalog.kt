package com.hazlosano.data.catalog

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.repository.CatalogRepository

/**
 * Un catálogo de mentira que contesta al instante y recuerda **con qué se le preguntó**.
 *
 * Guardar las peticiones es lo que permite afirmar lo que de verdad importa: que el filtro viaja,
 * que el término viaja, que la página crece y que no se pide dos veces la misma.
 *
 * Estaba privado dentro de `HomeFeedViewModelTest`. Sale aquí al aparecer el segundo ViewModel que
 * lo necesita —el de la búsqueda— porque copiarlo habría sido el segundo doble casi idéntico, con
 * dos sitios donde arreglar el mismo detalle del contrato.
 *
 * Para lo que ocurre **mientras** una petición viaja no sirve, y por eso no se usa allí: éste
 * contesta antes de que dé tiempo a que pase nada. Ver `PublicationPagerTest`.
 */
class FakeCatalog(
    private val pagesPerPillar: Int = 2,
    private val pageSize: Int = 20,
) : CatalogRepository {

    data class Ask(val pillar: PillarType?, val page: Int, val query: String = "")

    val asks: MutableList<Ask> = mutableListOf()

    /** Desde qué página se queda sin red. */
    var failFrom: Int? = null

    /** Desde qué página contesta con lo guardado en vez de con lo fresco. */
    var cachedFrom: Int? = null

    override suspend fun searchCatalog(
        query: String,
        pillar: PillarType?,
        page: Int,
        pageSize: Int,
        location: VisitorLocation?,
    ): CatalogPage {
        asks += Ask(pillar, page, query)
        failFrom?.let { if (page >= it) return CatalogPage.Unavailable }
        // Buscar no cae a lo guardado: la caché guarda publicaciones, no búsquedas. Por eso aquí no
        // se mira `cachedFrom` — hacerlo daría por bueno un comportamiento que el repositorio de
        // verdad no tiene.
        return CatalogPage.Fresh(
            publications = List(this.pageSize) { i -> product(pillar, page, i) },
            hasMore = page < pagesPerPillar,
        )
    }

    override suspend fun getCatalog(
        pillar: PillarType?,
        page: Int,
        pageSize: Int,
        location: VisitorLocation?,
    ): CatalogPage {
        asks += Ask(pillar, page)
        failFrom?.let { if (page >= it) return CatalogPage.Unavailable }
        cachedFrom?.let {
            if (page >= it) {
                return CatalogPage.Cached(List(this.pageSize) { i -> product(pillar, page, i) })
            }
        }
        return CatalogPage.Fresh(
            publications = List(this.pageSize) { i -> product(pillar, page, i) },
            hasMore = page < pagesPerPillar,
        )
    }

    private fun product(pillar: PillarType?, page: Int, index: Int) = HazloProduct(
        id = "${pillar?.key ?: "all"}-$page-$index",
        name = "p",
        description = "",
        price = null,
        imageUrl = "",
        category = "",
        pillar = pillar,
    )
}
