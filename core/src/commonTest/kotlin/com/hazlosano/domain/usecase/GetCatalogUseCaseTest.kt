package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.repository.CatalogRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetCatalogUseCaseTest {

    private class RecordingRepository(
        private val answer: CatalogPage = CatalogPage.Unavailable,
    ) : CatalogRepository {
        var pillar: PillarType? = null
        var page: Int? = null
        var pageSize: Int? = null
        var location: VisitorLocation? = null

        override suspend fun searchCatalog(
            query: String,
            pillar: PillarType?,
            page: Int,
            pageSize: Int,
            location: VisitorLocation?,
        ): CatalogPage = answer
        override suspend fun getCatalog(
            pillar: PillarType?,
            page: Int,
            pageSize: Int,
            location: VisitorLocation?,
        ): CatalogPage {
            this.pillar = pillar
            this.page = page
            this.pageSize = pageSize
            this.location = location
            return answer
        }
    }

    @Test
    fun `asks for the pillar it was given`() = runTest {
        val repository = RecordingRepository()

        GetCatalogUseCase(repository)(PillarType.MOVEMENT)

        assertEquals(PillarType.MOVEMENT, repository.pillar)
    }

    @Test
    fun `carries the location through untouched`() = runTest {
        val repository = RecordingRepository()
        val here = VisitorLocation(19.4326, -99.1332, fixedAtEpochMillis = 1_756_512_000_000L)

        GetCatalogUseCase(repository)(PillarType.NUTRITION, location = here)

        assertEquals(here, repository.location)
    }

    @Test
    fun `defaults to the first page and the app page size`() = runTest {
        val repository = RecordingRepository()

        GetCatalogUseCase(repository)(PillarType.SLEEP)

        assertEquals(1, repository.page)
        assertEquals(CatalogRepository.DEFAULT_PAGE_SIZE, repository.pageSize)
    }

    @Test
    fun `returns whatever the repository decided`() = runTest {
        val page = CatalogPage.Cached(emptyList())

        val result = GetCatalogUseCase(RecordingRepository(page))(PillarType.MIND)

        assertEquals(page, result)
    }
}
