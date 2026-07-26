package com.hazlosano.feature.movement.detail.presentation

import com.hazlosano.data.movement.trace.FakeTraceStore
import com.hazlosano.data.movement.trace.TraceStore
import com.hazlosano.domain.feature.movement.filter.DiscardReason
import com.hazlosano.domain.feature.movement.filter.TraceRecord
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionDetailUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class StubMovementSessionRepository(
    private val sessions: Flow<List<MovementSession>>,
    private val path: List<UserLocation> = emptyList(),
) : MovementSessionRepository {
    override fun getAllSessions(): Flow<List<MovementSession>> = sessions
    override fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> = flowOf(path)
    override suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsTheSummaryAndThePathOfTheSession() = runTest {
        val path = listOf(
            UserLocation(latitude = 19.43, longitude = -99.13),
            UserLocation(latitude = 19.44, longitude = -99.13),
        )
        val viewModel = buildViewModel(
            sessions = listOf(session()),
            path = path,
        )

        val state = assertIs<SessionDetailUiState.Detail>(viewModel.state.value)
        assertEquals("Sesión de movimiento", state.session.name)
        assertEquals("24 jul 2026 · 07:15", state.session.dateLabel)
        assertEquals("1.25 km", state.session.distanceLabel)
        assertEquals("10:00", state.session.durationLabel)
        assertEquals("8:00 /km", state.session.paceLabel)
        assertEquals("12 m", state.session.elevationLabel)
        assertEquals(path, state.session.path)
        assertTrue(state.session.hasPath)
    }

    @Test
    fun hasNoPathToDrawWhenTheSessionStoredNoPoints() = runTest {
        val viewModel = buildViewModel(sessions = listOf(session()), path = emptyList())

        val state = assertIs<SessionDetailUiState.Detail>(viewModel.state.value)
        assertFalse(state.session.hasPath)
        assertEquals("1.25 km", state.session.distanceLabel)
    }

    @Test
    fun reportsTheSessionAsMissingWhenItIsNotStored() = runTest {
        val viewModel = buildViewModel(sessions = emptyList())

        assertEquals(SessionDetailUiState.Missing, viewModel.state.value)
    }

    @Test
    fun showsAMessageWhenTheSessionCannotBeRead() = runTest {
        val viewModel = SessionDetailViewModel(
            sessionId = SESSION_ID,
            getSessionDetail = GetSessionDetailUseCase(
                StubMovementSessionRepository(
                    sessions = flow { throw IllegalStateException("base de datos no disponible") },
                ),
            ),
            timeZone = TimeZone.UTC,
        )

        val state = assertIs<SessionDetailUiState.Error>(viewModel.state.value)
        assertEquals("base de datos no disponible", state.message)
    }

    @Test
    fun explainsWhatTheFilterDidWithTheReadingsOfThisSession() = runTest {
        val path = listOf(
            UserLocation(latitude = 19.43, longitude = -99.13, timestamp = FIRST_POINT_MILLIS),
            UserLocation(latitude = 19.44, longitude = -99.13, timestamp = FIRST_POINT_MILLIS + 2_000),
        )
        val traceStore = FakeTraceStore()
        traceStore.finishedTraces[FIRST_POINT_MILLIS] = listOf(
            TraceRecord(reading(FIRST_POINT_MILLIS, accuracy = 10f), null),
            TraceRecord(reading(FIRST_POINT_MILLIS + 2_000, accuracy = 10f), null),
            TraceRecord(
                reading(FIRST_POINT_MILLIS + 4_000, accuracy = 10f),
                DiscardReason.WITHIN_NOISE,
            ),
            TraceRecord(
                reading(FIRST_POINT_MILLIS + 6_000, accuracy = 70f),
                DiscardReason.POOR_ACCURACY,
            ),
        )

        val viewModel = buildViewModel(listOf(session()), path, traceStore)

        val state = assertIs<SessionDetailUiState.Detail>(viewModel.state.value)
        val rows = assertNotNull(state.session.diagnosis).rows
        assertEquals("4", rows.value("Lecturas recibidas"))
        assertEquals("2 · 50 %", rows.value("Aceptadas"))
        assertEquals("2 · 50 %", rows.value("Descartadas"))
        assertEquals("1", rows.value("· Bajo el ruido"))
        assertEquals("1", rows.value("· Precisión insuficiente"))
        assertEquals("2.0 s", rows.value("Intervalo real"))
        assertTrue(
            rows.none { it.label == "· Salto imposible" },
            "a reason that never fired was listed as zero",
        )
    }

    @Test
    fun hasNoDiagnosisWhenTheSessionWasRecordedWithoutATrace() = runTest {
        val path = listOf(
            UserLocation(latitude = 19.43, longitude = -99.13, timestamp = FIRST_POINT_MILLIS),
        )

        val viewModel = buildViewModel(listOf(session()), path, FakeTraceStore())

        val state = assertIs<SessionDetailUiState.Detail>(viewModel.state.value)
        // Null rather than a diagnosis full of zeros, which would claim the filter rejected nothing.
        assertNull(state.session.diagnosis)
        assertEquals("1.25 km", state.session.distanceLabel)
    }

    private fun List<DiagnosisRow>.value(label: String): String? =
        firstOrNull { it.label == label }?.value

    private fun reading(timestamp: Long, accuracy: Float): UserLocation =
        UserLocation(
            latitude = 19.43,
            longitude = -99.13,
            accuracy = accuracy,
            timestamp = timestamp,
        )

    private fun buildViewModel(
        sessions: List<MovementSession>,
        path: List<UserLocation> = emptyList(),
        traceStore: TraceStore = FakeTraceStore(),
    ): SessionDetailViewModel =
        SessionDetailViewModel(
            sessionId = SESSION_ID,
            getSessionDetail = GetSessionDetailUseCase(
                StubMovementSessionRepository(sessions = flowOf(sessions), path = path),
            ),
            traceStore = traceStore,
            timeZone = TimeZone.UTC,
        )

    // 1.25 km in 10:00 is a pace of 8:00 /km.
    private fun session(): MovementSession =
        MovementSession(
            id = SESSION_ID,
            routeId = null,
            name = "Sesión de movimiento",
            date = JULY_24_2026_AT_07_15_UTC,
            elapsedTime = 600,
            distanceTraveled = 1_250.0,
            elevationGain = 12.0,
            previewPoints = emptyList(),
            stats = SessionStats(),
        )
}

private const val SESSION_ID = 7L
private const val JULY_24_2026_AT_07_15_UTC = 1_784_877_300_000L
private const val FIRST_POINT_MILLIS = 1_784_877_000_000L
