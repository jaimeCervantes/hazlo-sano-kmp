package com.hazlosano.feature.movement.detail

import com.hazlosano.data.db.inMemoryHazloSanoDatabase
import com.hazlosano.data.movement.SqlDelightMovementSessionRepository
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.model.boundingBox
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.usecase.GetSessionDetailUseCase
import com.hazlosano.domain.feature.movement.usecase.GetSessionsUseCase
import com.hazlosano.domain.feature.movement.usecase.RefreshSessionSummaryUseCase
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.time.TimeProvider
import com.hazlosano.feature.movement.detail.presentation.SessionDetailUiState
import com.hazlosano.feature.movement.detail.presentation.SessionDetailViewModel
import com.hazlosano.feature.movement.history.presentation.MovementHistoryUiState
import com.hazlosano.feature.movement.history.presentation.MovementHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
import kotlin.test.assertTrue

/**
 * Outer test for `features/movement_session_detail.feature`: a session recorded and saved through
 * the real persistence stack must open from the history with its stored route and its summary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailIntegrationTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun opensTheSessionListedInTheHistoryWithItsRouteAndSummary() = runTest {
        val repository: MovementSessionRepository =
            SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())
        saveRecordedSession(repository, path = RECORDED_PATH)

        val listedSessionId = historySessionIds(repository).single()
        val detail = detailOf(repository, listedSessionId)

        assertEquals("Sesión de movimiento", detail.session.name)
        assertEquals("24 jul 2026 · 07:15", detail.session.dateLabel)
        assertEquals("10:00", detail.session.durationLabel)
        // Measured from the route that came back out of the database, not from the 1250 m the
        // session was saved with.
        assertEquals("390 m", detail.session.distanceLabel)
        assertTrue(detail.session.hasPath)
        assertEquals(
            RECORDED_PATH.map { it.latitude to it.longitude },
            detail.session.path.map { it.latitude to it.longitude },
        )
    }

    @Test
    fun framesTheStoredRouteWithinItsBounds() = runTest {
        val repository: MovementSessionRepository =
            SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())
        saveRecordedSession(repository, path = RECORDED_PATH)

        val detail = detailOf(repository, historySessionIds(repository).single())

        val bounds = detail.session.path.boundingBox()!!
        assertTrue(bounds.spansAnArea)
        assertEquals(19.4300, bounds.minLatitude, 1e-6)
        assertEquals(19.4320, bounds.maxLatitude, 1e-6)
        assertEquals(-99.1320, bounds.minLongitude, 1e-6)
        assertEquals(-99.1300, bounds.maxLongitude, 1e-6)
    }

    @Test
    fun opensASessionThatStoredNoRoute() = runTest {
        val repository: MovementSessionRepository =
            SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())
        saveRecordedSession(repository, path = emptyList())

        val detail = detailOf(repository, historySessionIds(repository).single())

        assertEquals(0, detail.session.path.size)
        assertFalse(detail.session.hasPath)
        // Nothing was stored to measure, so nothing is claimed.
        assertEquals("—", detail.session.distanceLabel)
    }

    @Test
    fun openingASessionBringsTheHistorySummaryInLineWithItsRoute() = runTest {
        val repository: MovementSessionRepository =
            SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())
        // Saved with a distance that does not match the route it stored, as a session recorded
        // before the measurement improved would be.
        saveRecordedSession(repository, path = RECORDED_PATH)
        assertEquals("1.25 km", historyDistanceLabels(repository).single())

        detailOf(repository, historySessionIds(repository).single())

        // Se espera a que la fila cambie, en vez de leerla una vez.
        //
        // El ViewModel emite el detalle **antes** de refrescar el resumen —enseñar la sesión sin
        // esperar a la escritura es lo correcto para quien la abre— y ese refresco entra en
        // `Dispatchers.Default`, que `runTest` no controla. Leer el historial justo después es una
        // carrera que se gana casi siempre: el test pasó decenas de veces y falló una, informando
        // "1.25 km" porque la escritura todavía no había aterrizado.
        assertEquals("390 m", historyDistanceLabelOnceRefreshed(repository, expected = "390 m"))
    }

    /**
     * La distancia que el historial acaba enseñando, esperando a que la fila se actualice.
     *
     * El flujo de SQLDelight vuelve a emitir cuando la sesión se reescribe, así que basta con
     * quedarse en él hasta ver el valor esperado. Si nunca llega, `runTest` corta por su propio
     * tiempo límite — un fallo más feo que una aserción, pero fallo al fin, y sin la intermitencia
     * de leer una sola vez.
     */
    private suspend fun historyDistanceLabelOnceRefreshed(
        repository: MovementSessionRepository,
        expected: String,
    ): String {
        val history = MovementHistoryViewModel(
            getSessions = GetSessionsUseCase(repository),
            timeZone = TimeZone.UTC,
        )
        return history.state
            .filterIsInstance<MovementHistoryUiState.Sessions>()
            .map { it.items.single().distanceLabel }
            .first { it == expected }
    }

    private suspend fun historyDistanceLabels(
        repository: MovementSessionRepository,
    ): List<String> {
        val history = MovementHistoryViewModel(
            getSessions = GetSessionsUseCase(repository),
            timeZone = TimeZone.UTC,
        )
        val state = history.state.first { it !is MovementHistoryUiState.Loading }
        return assertIs<MovementHistoryUiState.Sessions>(state).items.map { it.distanceLabel }
    }

    private suspend fun historySessionIds(repository: MovementSessionRepository): List<Long> {
        val history = MovementHistoryViewModel(
            getSessions = GetSessionsUseCase(repository),
            timeZone = TimeZone.UTC,
        )
        val state = history.state.first { it !is MovementHistoryUiState.Loading }
        return assertIs<MovementHistoryUiState.Sessions>(state).items.map { it.id }
    }

    private suspend fun detailOf(
        repository: MovementSessionRepository,
        sessionId: Long,
    ): SessionDetailUiState.Detail {
        val viewModel = SessionDetailViewModel(
            sessionId = sessionId,
            getSessionDetail = GetSessionDetailUseCase(repository),
            refreshSessionSummary = RefreshSessionSummaryUseCase(repository),
            timeZone = TimeZone.UTC,
        )
        val state = viewModel.state.first { it !is SessionDetailUiState.Loading }
        return assertIs<SessionDetailUiState.Detail>(state)
    }

    private suspend fun saveRecordedSession(
        repository: MovementSessionRepository,
        path: List<UserLocation>,
    ) {
        val saveSession = SaveSessionUseCase(repository, TimeProvider { JULY_24_2026_AT_07_15_UTC })
        saveSession(
            name = "Sesión de movimiento",
            routeId = null,
            points = path,
            elapsedSeconds = 600,
            distanceMeters = 1_250.0,
        )
    }
}

private const val JULY_24_2026_AT_07_15_UTC = 1_784_877_300_000L

private val RECORDED_PATH = listOf(
    UserLocation(latitude = 19.4300, longitude = -99.1300, timestamp = 1_000),
    UserLocation(latitude = 19.4310, longitude = -99.1320, timestamp = 2_000),
    UserLocation(latitude = 19.4320, longitude = -99.1310, timestamp = 3_000),
)
