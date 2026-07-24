package com.hazlosano.feature.movement.history.presentation

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class StubMovementSessionRepository(
    private val sessions: Flow<List<MovementSession>>,
) : MovementSessionRepository {
    override fun getAllSessions(): Flow<List<MovementSession>> = sessions
    override fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> = flowOf(emptyList())
    override suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class MovementHistoryViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsAnEmptyHistoryWhenNoSessionWasSaved() = runTest {
        val viewModel = buildViewModel(flowOf(emptyList()))

        assertEquals(MovementHistoryUiState.Empty, viewModel.state.value)
    }

    @Test
    fun mapsASessionToItsDisplayLabels() = runTest {
        val viewModel = buildViewModel(
            flowOf(
                listOf(
                    session(
                        id = 7,
                        name = "Sesión de movimiento",
                        date = JULY_24_2026_AT_07_15_UTC,
                        distanceMeters = 1_250.0,
                        elapsedSeconds = 600,
                    ),
                ),
            ),
        )

        val state = assertIs<MovementHistoryUiState.Sessions>(viewModel.state.value)
        assertEquals(
            SessionListItem(
                id = 7,
                name = "Sesión de movimiento",
                dateLabel = "24 jul 2026 · 07:15",
                distanceLabel = "1.25 km",
                durationLabel = "10:00",
            ),
            state.items.single(),
        )
    }

    @Test
    fun listsTheMostRecentSessionFirstRegardlessOfRepositoryOrder() = runTest {
        val viewModel = buildViewModel(
            flowOf(
                listOf(
                    session(id = 1, name = "Ayer", date = JULY_24_2026_AT_07_15_UTC),
                    session(id = 2, name = "Hoy", date = JULY_24_2026_AT_07_15_UTC + ONE_DAY_MILLIS),
                ),
            ),
        )

        val state = assertIs<MovementHistoryUiState.Sessions>(viewModel.state.value)
        assertEquals(listOf("Hoy", "Ayer"), state.items.map { it.name })
    }

    @Test
    fun showsAMessageWhenTheHistoryCannotBeRead() = runTest {
        val viewModel = buildViewModel(flow { throw IllegalStateException("base de datos no disponible") })

        val state = assertIs<MovementHistoryUiState.Error>(viewModel.state.value)
        assertEquals("base de datos no disponible", state.message)
    }

    private fun buildViewModel(sessions: Flow<List<MovementSession>>): MovementHistoryViewModel =
        MovementHistoryViewModel(
            getSessions = GetSessionsUseCase(StubMovementSessionRepository(sessions)),
            timeZone = TimeZone.UTC,
        )

    private fun session(
        id: Long,
        name: String,
        date: Long,
        distanceMeters: Double = 1_000.0,
        elapsedSeconds: Long = 600,
    ): MovementSession =
        MovementSession(
            id = id,
            routeId = null,
            name = name,
            date = date,
            elapsedTime = elapsedSeconds,
            distanceTraveled = distanceMeters,
            elevationGain = 0.0,
            previewPoints = emptyList(),
            stats = SessionStats(),
        )
}

private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1_000L
private const val JULY_24_2026_AT_07_15_UTC = 1_784_877_300_000L
