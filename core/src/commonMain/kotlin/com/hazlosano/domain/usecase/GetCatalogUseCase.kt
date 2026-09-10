package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.repository.CatalogRepository

/**
 * El catálogo, listo para pintar: de un pilar, o de los cuatro.
 *
 * **[pillar] es opcional porque lo es en el sitio.** Se llamaba `GetPillarCatalogUseCase` cuando el
 * único sitio que lo pedía era la pestaña de un pilar; el feed del Inicio pide lo mismo sin filtro,
 * y hacer un segundo caso de uso para eso habría sido dos nombres para una operación.
 *
 * La ubicación se pide **al invocar y no al construir**: cambia entre una apertura y la siguiente, y
 * guardarla en el caso de uso la congelaría en la primera lectura.
 */
class GetCatalogUseCase(
    private val repository: CatalogRepository,
) {
    suspend operator fun invoke(
        pillar: PillarType? = null,
        location: VisitorLocation? = null,
        page: Int = 1,
        pageSize: Int = CatalogRepository.DEFAULT_PAGE_SIZE,
    ): CatalogPage = repository.getCatalog(
        pillar = pillar,
        page = page,
        pageSize = pageSize,
        location = location,
    )
}
