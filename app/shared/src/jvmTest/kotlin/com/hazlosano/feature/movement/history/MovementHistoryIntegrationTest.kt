package com.hazlosano.feature.movement.history

import com.hazlosano.data.db.inMemoryHazloSanoDatabase
import com.hazlosano.data.movement.SqlDelightMovementSessionRepository
import com.hazlosano.domain.feature.movement.model.NavigationState
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionsUseCase
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.time.TimeProvider
import com.hazlosano.feature.movement.history.presentation.MovementHistoryUiState
import com.hazlosano.feature.movement.history.presentation.MovementHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Outer test for `features/movement_history.feature`: a session recorded and saved through the
 * real persistence stack must come back as a row in the history, newest first.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovementHistoryIntegrationTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun listsARecordedSessionWithItsDateDistanceAndTime() = runTest {
        val repository: MovementSessionRepository =
            SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())
        saveRecordedSession(repository, at = JULY_24_AT_07_15)

        val viewModel = historyViewModel(repository)

        val sessions = viewModel.awaitSessions()
        assertEquals(1, sessions.items.size)
        val item = sessions.items.single()
        assertEquals("Sesión de movimiento", item.name)
        assertEquals("24 jul 2026 · 07:15", item.dateLabel)
        assertEquals("1.25 km", item.distanceLabel)
        assertEquals("10:00", item.durationLabel)
    }

    @Test
    fun listsTheMostRecentSessionFirst() = runTest {
        val repository: MovementSessionRepository =
            SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())
        saveRecordedSession(repository, at = JULY_24_AT_07_15, name = "Ayer")
        saveRecordedSession(repository, at = JULY_25_AT_07_15, name = "Hoy")

        val sessions = historyViewModel(repository).awaitSessions()

        assertEquals(listOf("Hoy", "Ayer"), sessions.items.map { it.name })
    }

    private fun historyViewModel(repository: MovementSessionRepository): MovementHistoryViewModel =
        MovementHistoryViewModel(
            getSessions = GetSessionsUseCase(repository),
            timeZone = TimeZone.UTC,
        )

    private suspend fun MovementHistoryViewModel.awaitSessions(): MovementHistoryUiState.Sessions {
        val state = state.first { it !is MovementHistoryUiState.Loading }
        assertIs<MovementHistoryUiState.Sessions>(state)
        return state
    }

    private suspend fun saveRecordedSession(
        repository: MovementSessionRepository,
        at: Long,
        name: String = "Sesión de movimiento",
    ) {
        val saveSession = SaveSessionUseCase(repository, TimeProvider { at })
        saveSession(
            name = name,
            routeId = null,
            state = NavigationState(
                traveledPoints = listOf(
                    UserLocation(latitude = 19.4300, longitude = -99.1300, timestamp = at),
                    UserLocation(latitude = 19.4310, longitude = -99.1300, timestamp = at + 1_000),
                ),
                elapsedTime = 600,
                distanceTraveled = 1_250.0,
                elevationGain = 5.0,
                stats = SessionStats(totalAscent = 5.0, avgPace = 8.0, movingTime = 580),
            ),
        )
    }
}

private val JULY_24_AT_07_15 =
    LocalDateTime(2026, 7, 24, 7, 15).toInstant(TimeZone.UTC).toEpochMilliseconds()
private val JULY_25_AT_07_15 =
    LocalDateTime(2026, 7, 25, 7, 15).toInstant(TimeZone.UTC).toEpochMilliseconds()
