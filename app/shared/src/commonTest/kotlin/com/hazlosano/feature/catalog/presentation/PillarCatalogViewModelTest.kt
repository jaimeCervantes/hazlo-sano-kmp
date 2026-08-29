package com.hazlosano.feature.catalog.presentation

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.repository.CatalogRepository
import com.hazlosano.domain.usecase.GetPillarCatalogUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PillarCatalogViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private class StubRepository(
        private val answer: CatalogPage,
        private val explode: Boolean = false,
    ) : CatalogRepository {
        var seenLocation: VisitorLocation? = null
        var calls = 0

        override suspend fun getPillarCatalog(
            pillar: PillarType,
            page: Int,
            pageSize: Int,
            location: VisitorLocation?,
        ): CatalogPage {
            calls++
            seenLocation = location
            if (explode) throw IllegalStateException("boom")
            return answer
        }
    }

    private fun viewModel(
        repository: CatalogRepository,
        location: suspend () -> VisitorLocation? = { null },
    ) = PillarCatalogViewModel(
        pillar = PillarType.NUTRITION,
        getPillarCatalog = GetPillarCatalogUseCase(repository),
        readLocation = location,
    )

    private val onePublication = listOf(HazloProduct(id = "p1", name = "Suero natural", price = 45.0))

    @Test
    fun `a fresh page is ready and not marked stale`() = runTest {
        val state = viewModel(StubRepository(CatalogPage.Fresh(onePublication))).uiState.value

        val ready = assertIs<PillarCatalogUiState.Ready>(state)
        assertEquals("Suero natural", ready.sections.all.single().name)
        assertFalse(ready.fromCache)
    }

    @Test
    fun `a cached page is ready and says it is stale`() = runTest {
        val state = viewModel(StubRepository(CatalogPage.Cached(onePublication))).uiState.value

        val ready = assertIs<PillarCatalogUiState.Ready>(state)
        assertTrue(ready.fromCache)
    }

    @Test
    fun `nothing read and no network becomes unavailable`() = runTest {
        val state = viewModel(StubRepository(CatalogPage.Unavailable)).uiState.value

        assertEquals(PillarCatalogUiState.Unavailable, state)
    }

    @Test
    fun `an unexpected failure is not disguised as being offline`() = runTest {
        val state = viewModel(StubRepository(CatalogPage.Unavailable, explode = true)).uiState.value

        assertEquals(PillarCatalogUiState.Failed, state)
    }

    @Test
    fun `the location it read travels to the repository`() = runTest {
        val here = VisitorLocation(19.4326, -99.1332)
        val repository = StubRepository(CatalogPage.Fresh(onePublication))

        viewModel(repository) { here }

        assertEquals(here, repository.seenLocation)
    }

    @Test
    fun `a location that cannot be read does not cancel the catalogue`() = runTest {
        val repository = StubRepository(CatalogPage.Fresh(onePublication))

        val state = viewModel(repository) { throw IllegalStateException("no permission") }.uiState.value

        // Sin ubicación el catálogo sale por fecha; quedarse en blanco sería peor.
        assertIs<PillarCatalogUiState.Ready>(state)
        assertEquals(null, repository.seenLocation)
    }

    @Test
    fun `refresh asks again`() = runTest {
        val repository = StubRepository(CatalogPage.Fresh(onePublication))
        val vm = viewModel(repository)

        vm.refresh()

        assertEquals(2, repository.calls)
    }
}
