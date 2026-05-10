package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.SleepPhase
import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.model.SleepSource
import com.hazlosano.kmp.domain.repository.SleepSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSleepAnalysisUseCaseTest {

    private val fakeRepo = FakeSleepSessionRepository()
    private val useCase = GetSleepAnalysisUseCase(fakeRepo)

    // ── existing tests ──

    @Test
    fun `empty repo returns zero analysis`() = runTest {
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(0, result.totalSessions)
        assertEquals(0L, result.totalDurationMillis)
        assertEquals(0f, result.efficiency)
    }

    @Test
    fun `single session calculates efficiency correctly`() = runTest {
        fakeRepo.sessions.add(
            SleepSession("1", 100L, 500L, SleepSource.PHONE_SENSORS, 1.0f),
        )
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(1, result.totalSessions)
        assertEquals(400L, result.totalDurationMillis)
        assertEquals(0.4f, result.efficiency, 0.001f)
    }

    @Test
    fun `multiple sessions sum durations`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 100L, 300L, SleepSource.PHONE_SENSORS, 1.0f),
                SleepSession("2", 400L, 600L, SleepSource.PHONE_SENSORS, 1.0f),
            ),
        )
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(400L, result.totalDurationMillis)
    }

    @Test
    fun `session duration is clipped to query window`() = runTest {
        fakeRepo.sessions.add(
            SleepSession("1", -100L, 500L, SleepSource.PHONE_SENSORS, 1.0f),
        )
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(500L, result.totalDurationMillis)
    }

    // ── BDD: clustering ──

    @Test
    fun `given two clusters with gap over 2h main cluster is used for times`() = runTest {
        // outlier: phone idle at 7 PM
        fakeRepo.sessions.add(
            SleepSession("outlier", 19 * 3600_000L, 20 * 3600_000L, SleepSource.PHONE_SENSORS, 1.0f),
        )
        // main sleep: midnight to 6 AM
        fakeRepo.sessions.add(
            SleepSession("main", 24 * 3600_000L, 30 * 3600_000L, SleepSource.PHONE_SENSORS, 1.0f),
        )

        val result = useCase(from = 0L, to = 48 * 3600_000L)

        assertEquals(2, result.totalSessions)
        // firstSleepStart should be from main cluster (midnight), NOT the outlier (7 PM)
        assertEquals(24 * 3600_000L, result.firstSleepStart)
        assertEquals(30 * 3600_000L, result.lastSleepEnd)
        // total duration still includes both
        assertEquals(7 * 3600_000L, result.totalDurationMillis)
    }

    @Test
    fun `given segments close together they form single cluster`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 23 * 3600_000L, 25 * 3600_000L, SleepSource.PHONE_SENSORS, 1.0f),
                SleepSession("2", 25 * 3600_000L + 30 * 60_000L, 28 * 3600_000L,
                    SleepSource.PHONE_SENSORS, 1.0f),
            ),
        )
        val result = useCase(from = 0L, to = 48 * 3600_000L)

        // 30 min gap < 2h, so same cluster
        assertEquals(23 * 3600_000L, result.firstSleepStart)
        assertEquals(28 * 3600_000L, result.lastSleepEnd)
        assertEquals(2, result.totalSessions)
    }

    @Test
    fun `given one segment it is the main cluster`() = runTest {
        fakeRepo.sessions.add(
            SleepSession("only", 100L, 500L, SleepSource.PHONE_SENSORS, 1.0f),
        )
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(100L, result.firstSleepStart)
        assertEquals(500L, result.lastSleepEnd)
    }

    // ── BDD: phase breakdown ──

    @Test
    fun `given sessions with different phases breakdown groups by phase`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 0L, 3000L, SleepSource.PHONE_SENSORS, 1.0f,
                    phase = SleepPhase.LIGHT),
                SleepSession("2", 3000L, 5000L, SleepSource.PHONE_SENSORS, 1.0f,
                    phase = SleepPhase.DEEP),
                SleepSession("3", 5000L, 6000L, SleepSource.PHONE_SENSORS, 1.0f,
                    phase = SleepPhase.REM),
            ),
        )
        val result = useCase(from = 0L, to = 10000L)

        assertEquals(3000L, result.phaseBreakdown[SleepPhase.LIGHT])
        assertEquals(2000L, result.phaseBreakdown[SleepPhase.DEEP])
        assertEquals(1000L, result.phaseBreakdown[SleepPhase.REM])
    }

    @Test
    fun `given sessions with UNKNOWN and AWAKE phases they are excluded from breakdown`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 0L, 1000L, SleepSource.PHONE_SENSORS, 1.0f,
                    phase = SleepPhase.AWAKE),
                SleepSession("2", 1000L, 3000L, SleepSource.PHONE_SENSORS, 1.0f,
                    phase = SleepPhase.UNKNOWN),
                SleepSession("3", 3000L, 5000L, SleepSource.PHONE_SENSORS, 1.0f,
                    phase = SleepPhase.LIGHT),
            ),
        )
        val result = useCase(from = 0L, to = 10000L)

        assertTrue(SleepPhase.AWAKE !in result.phaseBreakdown)
        assertTrue(SleepPhase.UNKNOWN !in result.phaseBreakdown)
        assertEquals(2000L, result.phaseBreakdown[SleepPhase.LIGHT])
    }

    // ── BDD: confidence ──

    @Test
    fun `given sessions with different confidence average is computed`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 0L, 1000L, SleepSource.PHONE_SENSORS, 0.8f),
                SleepSession("2", 1000L, 2000L, SleepSource.PHONE_SENSORS, 1.0f),
            ),
        )
        val result = useCase(from = 0L, to = 10000L)

        assertEquals(0.9f, result.averageConfidence, 0.001f)
    }

    @Test
    fun `given sessions with default confidence average is 1f`() = runTest {
        fakeRepo.sessions.add(
            SleepSession("1", 0L, 1000L, SleepSource.PHONE_SENSORS),
        )
        val result = useCase(from = 0L, to = 10000L)

        assertEquals(1.0f, result.averageConfidence)
    }
}

private class FakeSleepSessionRepository : SleepSessionRepository {
    val sessions = mutableListOf<SleepSession>()

    override suspend fun getSleepSessions(from: Long, to: Long): List<SleepSession> =
        sessions.filter { it.endTime > from && it.startTime < to }

    override suspend fun saveSleepSession(session: SleepSession) {
        sessions.add(session)
    }
}
