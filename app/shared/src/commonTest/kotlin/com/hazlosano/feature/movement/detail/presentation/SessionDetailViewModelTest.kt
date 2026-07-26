package com.hazlosano.feature.movement.detail.presentation

import com.hazlosano.data.movement.trace.FakeTraceStore
import com.hazlosano.data.movement.trace.TraceStore
import com.hazlosano.domain.feature.movement.filter.DiscardReason
import com.hazlosano.domain.feature.movement.filter.TraceRecord
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionDetailUseCase
import com.hazlosano.domain.feature.movement.usecase.RefreshSessionSummaryUseCase
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

    val distanceUpdates: MutableList<Pair<Long, Double>> = mutableListOf()

    override suspend fun updateDistance(sessionId: Long, distanceMeters: Double) {
        distanceUpdates += sessionId to distanceMeters
    }
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
        val viewModel = buildViewModel(sessions = listOf(session()), path = climbingPath())

        val state = assertIs<SessionDetailUiState.Detail>(viewModel.state.value)
        assertEquals("Sesión de movimiento", state.session.name)
        assertEquals("24 jul 2026 · 07:15", state.session.dateLabel)
        // Measured from the route, not read from the 1250 m stored with the session.
        assertEquals("1.11 km", state.session.distanceLabel)
        assertEquals("10:00", state.session.durationLabel)
        assertEquals(climbingPath(), state.session.path)
        assertTrue(state.session.hasPath)
    }

    @Test
    fun reportsEverythingTheRouteShows() = runTest {
        val viewModel = buildViewModel(sessions = listOf(session()), path = climbingPath())

        val state = assertIs<SessionDetailUiState.Detail>(viewModel.state.value)
        val labels = state.session.metrics.map { it.label }
        assertEquals(
            listOf(
                "Distancia", "Tiempo", "En movimiento", "Ritmo",
                "Desnivel +", "Desnivel −", "Altitud máx.", "Altitud mín.",
            ),
            labels,
        )
        assertEquals("2100 m", state.session.maxAltitudeLabel)
        assertEquals("2000 m", state.session.minAltitudeLabel)
        assertTrue(state.session.movingTimeLabel != NO_VALUE, "time spent moving was not measured")
    }

    @Test
    fun readsAsEmptyWhereNothingWasMeasured() = runTest {
        // Readings without altitude: no climb rather than a climb of zero, which would claim the
        // outing was flat.
        val flat = listOf(
            UserLocation(latitude = 19.4300, longitude = -99.13, timestamp = 0),
            UserLocation(latitude = 19.4400, longitude = -99.13, timestamp = 60_000),
        )

        val viewModel = buildViewModel(sessions = listOf(session()), path = flat)

        val state = assertIs<SessionDetailUiState.Detail>(viewModel.state.value)
        assertEquals(NO_VALUE, state.session.ascentLabel)
        assertEquals(NO_VALUE, state.session.descentLabel)
        assertEquals(NO_VALUE, state.session.maxAltitudeLabel)
        assertEquals(NO_VALUE, state.session.minAltitudeLabel)
        assertEquals("1.11 km", state.session.distanceLabel)
    }

    @Test
    fun aSessionThatRecordedNothingReportsNothing() = runTest {
        val viewModel = buildViewModel(sessions = listOf(session()), path = emptyList())

        val state = assertIs<SessionDetailUiState.Detail>(viewModel.state.value)
        assertFalse(state.session.hasPath)
        assertTrue(
            state.session.metrics.filter { it.label != "Tiempo" }.all { it.value == NO_VALUE },
            "a session with no route reported ${state.session.metrics}",
        )
    }

    @Test
    fun refreshesTheSummaryTheHistoryListsWhenTheRouteSaysOtherwise() = runTest {
        val repository = StubMovementSessionRepository(
            sessions = flowOf(listOf(session())),
            path = climbingPath(),
        )

        buildViewModel(repository = repository)

        // The stored summary said 1250 m; the route says 1113 m. The history is brought in line so
        // the two screens do not disagree.
        val (sessionId, distance) = repository.distanceUpdates.single()
        assertEquals(SESSION_ID, sessionId)
        assertTrue(distance in 1_100.0..1_120.0, "healed the summary to $distance m")
    }

    @Test
    fun leavesTheSummaryAloneWhenTheRouteAgreesWithIt() = runTest {
        val repository = StubMovementSessionRepository(
            sessions = flowOf(listOf(session(distanceMeters = MEASURED_ROUTE_METERS))),
            path = climbingPath(),
        )

        buildViewModel(repository = repository)

        assertTrue(
            repository.distanceUpdates.isEmpty(),
            "rewrote the history for ${repository.distanceUpdates}",
        )
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
            refreshSessionSummary = RefreshSessionSummaryUseCase(
                StubMovementSessionRepository(sessions = flowOf(emptyList())),
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
        sessions: List<MovementSession> = listOf(session()),
        path: List<UserLocation> = emptyList(),
        traceStore: TraceStore = FakeTraceStore(),
        repository: StubMovementSessionRepository =
            StubMovementSessionRepository(sessions = flowOf(sessions), path = path),
    ): SessionDetailViewModel =
        SessionDetailViewModel(
            sessionId = SESSION_ID,
            getSessionDetail = GetSessionDetailUseCase(repository),
            refreshSessionSummary = RefreshSessionSummaryUseCase(repository),
            traceStore = traceStore,
            timeZone = TimeZone.UTC,
        )

    /** A hundred metres of climb over 0.01° of latitude, which is [MEASURED_ROUTE_METERS] apart. */
    private fun climbingPath(): List<UserLocation> = listOf(
        UserLocation(latitude = 19.4300, longitude = -99.13, altitude = 2_000.0, timestamp = 0),
        UserLocation(latitude = 19.4350, longitude = -99.13, altitude = 2_050.0, timestamp = 30_000),
        UserLocation(latitude = 19.4400, longitude = -99.13, altitude = 2_100.0, timestamp = 60_000),
    )

    private fun session(distanceMeters: Double = 1_250.0): MovementSession =
        MovementSession(
            id = SESSION_ID,
            routeId = null,
            name = "Sesión de movimiento",
            date = JULY_24_2026_AT_07_15_UTC,
            elapsedTime = 600,
            distanceTraveled = distanceMeters,
            previewPoints = emptyList(),
        )
}

/** What the app shows where nothing was measured. */
private const val NO_VALUE = "—"

/** What `climbingPath()` measures, so a test can store a summary that already agrees with it. */
private const val MEASURED_ROUTE_METERS = 1_111.9

private const val SESSION_ID = 7L
private const val JULY_24_2026_AT_07_15_UTC = 1_784_877_300_000L
private const val FIRST_POINT_MILLIS = 1_784_877_000_000L
