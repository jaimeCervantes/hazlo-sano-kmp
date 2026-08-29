package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.repository.CatalogRepository

/**
 * El catálogo de un pilar, listo para pintar.
 *
 * La ubicación se pide **al invocar y no al construir**: cambia entre una apertura y la siguiente, y
 * guardarla en el caso de uso la congelaría en la primera lectura.
 */
class GetPillarCatalogUseCase(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(
        pillar: PillarType,
        location: VisitorLocation? = null,
        page: Int = 1,
        pageSize: Int = CatalogRepository.DEFAULT_PAGE_SIZE,
    ): CatalogPage = repository.getPillarCatalog(
        pillar = pillar,
        page = page,
        pageSize = pageSize,
        location = location,
    )
}
