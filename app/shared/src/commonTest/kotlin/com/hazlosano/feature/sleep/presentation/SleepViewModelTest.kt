package com.hazlosano.feature.sleep.presentation

import com.hazlosano.domain.model.SleepSession
import com.hazlosano.domain.repository.SleepSessionRepository
import com.hazlosano.domain.usecase.GetSleepAnalysisUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Antes de este slice, `refresh()` sólo actuaba si el estado ya era `Success`: un fallo no se podía
 * reintentar sin salir de la pantalla. Lo que se comprueba aquí es que, desde `Failed`, `refresh()`
 * vuelve a cargar como al abrir la pantalla.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SleepViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private class StubRepository(var explode: Boolean) : SleepSessionRepository {
        val sessions = mutableListOf<SleepSession>()

        override suspend fun getSleepSessions(from: Long, to: Long): List<SleepSession> {
            if (explode) throw IllegalStateException("boom")
            return sessions.filter { it.endTime > from && it.startTime < to }
        }

        override suspend fun saveSleepSession(session: SleepSession) {
            sessions.add(session)
        }
    }

    @Test
    fun `a failure to read last night is reported as failed`() = runTest {
        val repository = StubRepository(explode = true)
        val viewModel = SleepViewModel(GetSleepAnalysisUseCase(repository), repository)

        assertIs<SleepUiState.Failed>(viewModel.uiState.value)
    }

    @Test
    fun `refresh recovers from a failure once the night is reachable`() = runTest {
        val repository = StubRepository(explode = true)
        val viewModel = SleepViewModel(GetSleepAnalysisUseCase(repository), repository)
        assertIs<SleepUiState.Failed>(viewModel.uiState.value)

        repository.explode = false
        viewModel.refresh()

        assertIs<SleepUiState.Success>(viewModel.uiState.value)
    }
}
