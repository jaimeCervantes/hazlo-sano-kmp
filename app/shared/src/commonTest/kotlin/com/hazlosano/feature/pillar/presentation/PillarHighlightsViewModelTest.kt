package com.hazlosano.feature.pillar.presentation

import com.hazlosano.domain.model.HazloChampion
import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.PillarHighlightsRepository
import com.hazlosano.domain.usecase.GetPillarHighlightsUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class PillarHighlightsViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private class StubRepository(
        private val answer: PillarHighlights = PillarHighlights(),
        private val explode: Boolean = false,
    ) : PillarHighlightsRepository {
        var calls = 0
        var asked: PillarType? = null

        override suspend fun getHighlights(pillar: PillarType): PillarHighlights {
            calls++
            asked = pillar
            if (explode) throw IllegalStateException("boom")
            return answer
        }
    }

    private fun viewModel(
        repository: PillarHighlightsRepository,
        pillar: PillarType = PillarType.MOVEMENT,
    ) = PillarHighlightsViewModel(
        pillar = pillar,
        getPillarHighlights = GetPillarHighlightsUseCase(repository),
    )

    private val oneChampion = PillarHighlights(
        champions = listOf(
            HazloChampion(name = "Mateo R.", title = "Top Trekker", stat = "42.5 km", imageUrl = ""),
        ),
    )

    @Test
    fun `what it read is ready to paint`() = runTest {
        val state = viewModel(StubRepository(oneChampion)).uiState.value

        val ready = assertIs<PillarHighlightsUiState.Ready>(state)
        assertEquals("Mateo R.", ready.highlights.champions.single().name)
    }

    @Test
    fun `it asks for the pillar it belongs to`() = runTest {
        val repository = StubRepository(oneChampion)

        viewModel(repository, PillarType.MIND)

        assertEquals(PillarType.MIND, repository.asked)
    }

    @Test
    fun `a failure does not invent champions`() = runTest {
        val state = viewModel(StubRepository(explode = true)).uiState.value

        assertEquals(PillarHighlightsUiState.Unavailable, state)
    }

    @Test
    fun `refresh asks again`() = runTest {
        val repository = StubRepository(oneChampion)
        val vm = viewModel(repository)

        vm.refresh()

        assertEquals(2, repository.calls)
    }
}
