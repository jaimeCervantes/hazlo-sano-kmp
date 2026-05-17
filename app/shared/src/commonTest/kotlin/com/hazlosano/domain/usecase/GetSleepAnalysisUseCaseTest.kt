package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.SleepPhase
import com.hazlosano.domain.model.SleepSession
import com.hazlosano.domain.model.SleepSource
import com.hazlosano.domain.repository.SleepSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSleepAnalysisUseCaseTest {

    private val fakeRepo = FakeSleepSessionRepository()
    private val useCase = GetSleepAnalysisUseCase(fakeRepo)

    @Test
    fun `empty repo returns zero analysis`() = runTest {
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(0, result.totalSessions)
        assertEquals(0L, result.totalDurationMillis)
        assertEquals(0f, result.efficiency)
    }

    @Test
    fun `single uninterrupted session gives 100 percent efficiency`() = runTest {
        fakeRepo.sessions.add(
            SleepSession("1", 100L, 500L, SleepSource.PHONE_SENSORS, 1.0f),
        )
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(1, result.totalSessions)
        assertEquals(400L, result.totalDurationMillis)
        // timeInBed = 500-100 = 400, sleep = 400, efficiency = 400/400
        assertEquals(1.0f, result.efficiency, 0.001f)
    }

    @Test
    fun `sessions with awake gaps reduce efficiency`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 100L, 300L, SleepSource.PHONE_SENSORS, 1.0f),
                // awake gap of 100ms
                SleepSession("2", 400L, 600L, SleepSource.PHONE_SENSORS, 1.0f),
            ),
        )
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(2, result.totalSessions)
        assertEquals(400L, result.totalDurationMillis)
        // sleep period: 600-100 = 500, sleep: 200+200 = 400, efficiency: 400/500
        assertEquals(0.8f, result.efficiency, 0.001f)
    }

    @Test
    fun `duration clipped to query window boundaries`() = runTest {
        fakeRepo.sessions.add(
            SleepSession("1", -100L, 500L, SleepSource.PHONE_SENSORS, 1.0f),
        )
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(500L, result.totalDurationMillis)
        assertEquals(1.0f, result.efficiency, 0.001f)
    }

    @Test
    fun `all sessions count regardless of gap size`() = runTest {
        // early evening nap
        fakeRepo.sessions.add(
            SleepSession("nap", 19 * 3600_000L, 20 * 3600_000L, SleepSource.PHONE_SENSORS, 1.0f),
        )
        // main night sleep (4h gap - real awake time)
        fakeRepo.sessions.add(
            SleepSession("night", 24 * 3600_000L, 30 * 3600_000L, SleepSource.PHONE_SENSORS, 1.0f),
        )

        val result = useCase(from = 0L, to = 48 * 3600_000L)

        assertEquals(2, result.totalSessions)
        // total sleep = 1h + 6h = 7h
        assertEquals(7 * 3600_000L, result.totalDurationMillis)
        // period = 6AM - 7PM = 11h
        assertEquals(19 * 3600_000L, result.firstSleepStart)
        assertEquals(30 * 3600_000L, result.lastSleepEnd)
        // efficiency = 7h/11h = 63.6%
        assertEquals(0.636f, result.efficiency, 0.01f)
    }

    @Test
    fun `interrupted sleep reflects real efficiency`() = runTest {
        // simulates user's case: slept early, woke to work, slept again
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 19 * 3600_000L + 8 * 60_000L,           // 7:08 PM
                    22 * 3600_000L + 26 * 60_000L,                         // 10:26 PM
                    SleepSource.PHONE_SENSORS, 1.0f),
                SleepSession("2", 24 * 3600_000L + 50 * 60_000L,           // 12:50 AM
                    28 * 3600_000L + 56 * 60_000L,                         // 4:56 AM
                    SleepSource.PHONE_SENSORS, 1.0f),
                SleepSession("3", 29 * 3600_000L + 10 * 60_000L,           // 5:10 AM
                    30 * 3600_000L + 2 * 60_000L,                          // 6:02 AM
                    SleepSource.PHONE_SENSORS, 1.0f),
            ),
        )
        val result = useCase(from = 0L, to = 48 * 3600_000L)

        assertEquals(3, result.totalSessions)
        // total sleep ≈ 8h 16m
        val expectedSleepMs = (3 * 3600_000L + 18 * 60_000L) +
            (4 * 3600_000L + 6 * 60_000L) +
            (52 * 60_000L)
        assertEquals(expectedSleepMs, result.totalDurationMillis)
        // period: 6:02 AM - 7:08 PM
        val expectedPeriodMs = (30 * 3600_000L + 2 * 60_000L) -
            (19 * 3600_000L + 8 * 60_000L)
        assertEquals(expectedPeriodMs, result.lastSleepEnd!! - result.firstSleepStart!!)
        // efficiency ≈ 75.8%
        assertEquals(0.758f, result.efficiency, 0.01f)
    }

    // ── phase breakdown ──

    @Test
    fun `phase breakdown groups by phase`() = runTest {
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
    fun `AWAKE and UNKNOWN excluded from breakdown`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 0L, 1000L, SleepSource.PHONE_SENSORS, 1.0f,
                    phase = SleepPhase.AWAKE),
                SleepSession("2", 3000L, 5000L, SleepSource.PHONE_SENSORS, 1.0f,
                    phase = SleepPhase.LIGHT),
            ),
        )
        val result = useCase(from = 0L, to = 10000L)

        assertTrue(SleepPhase.AWAKE !in result.phaseBreakdown)
        assertEquals(2000L, result.phaseBreakdown[SleepPhase.LIGHT])
    }

    // ── confidence ──

    @Test
    fun `average confidence across all sessions`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 0L, 1000L, SleepSource.PHONE_SENSORS, 0.8f),
                SleepSession("2", 1000L, 2000L, SleepSource.PHONE_SENSORS, 1.0f),
            ),
        )
        val result = useCase(from = 0L, to = 10000L)

        assertEquals(0.9f, result.averageConfidence, 0.001f)
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
