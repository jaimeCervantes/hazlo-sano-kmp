package com.hazlosano.feature.home.presentation

import com.hazlosano.domain.model.HomeContent
import com.hazlosano.domain.repository.HomeRepository
import com.hazlosano.domain.usecase.GetHomeContentUseCase
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

/**
 * Antes de este slice, `HomeViewModel` no tenía forma de reintentar tras un fallo: había que salir
 * de la pantalla. Lo que se comprueba aquí es que `refresh()` vuelve a pedir el contenido con la
 * misma carga que corre al abrir Inicio.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val content = HomeContent(
        headerTitle = "Tu Ecosistema",
        headerSubtitle = "Cultiva tus 4 pilares hoy",
        pillars = emptyList(),
        champions = emptyList(),
        feedPosts = emptyList(),
    )

    private class StubRepository(var explode: Boolean, private val content: HomeContent) : HomeRepository {
        var calls = 0

        override suspend fun getHomeContent(): HomeContent {
            calls++
            if (explode) throw IllegalStateException("boom")
            return content
        }
    }

    @Test
    fun `a failure to load is reported as failed`() = runTest {
        val repository = StubRepository(explode = true, content = content)
        val viewModel = HomeViewModel(GetHomeContentUseCase(repository))

        assertIs<HomeUiState.Failed>(viewModel.uiState.value)
    }

    @Test
    fun `refresh asks again`() = runTest {
        val repository = StubRepository(explode = false, content = content)
        val viewModel = HomeViewModel(GetHomeContentUseCase(repository))

        viewModel.refresh()

        assertEquals(2, repository.calls)
    }

    @Test
    fun `refresh recovers from a failure once the content is reachable`() = runTest {
        val repository = StubRepository(explode = true, content = content)
        val viewModel = HomeViewModel(GetHomeContentUseCase(repository))
        assertIs<HomeUiState.Failed>(viewModel.uiState.value)

        repository.explode = false
        viewModel.refresh()

        assertIs<HomeUiState.Success>(viewModel.uiState.value)
    }
}
