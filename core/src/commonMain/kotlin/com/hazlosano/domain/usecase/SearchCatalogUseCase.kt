package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.repository.CatalogRepository

/**
 * Buscar publicaciones por texto, opcionalmente dentro de un pilar.
 *
 * Va aparte de [GetCatalogUseCase] por lo mismo que en el repositorio: al otro lado son dos
 * endpoints con dos contratos, y esconder eso tras un parámetro opcional haría que una de las dos
 * mitades mintiera sobre lo que sabe.
 */
class SearchCatalogUseCase(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(
        query: String,
        pillar: PillarType? = null,
        location: VisitorLocation? = null,
        page: Int = 1,
        pageSize: Int = CatalogRepository.DEFAULT_PAGE_SIZE,
    ): CatalogPage = repository.searchCatalog(
        query = query,
        pillar = pillar,
        page = page,
        pageSize = pageSize,
        location = location,
    )
}
