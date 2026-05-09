package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.model.SleepSource
import com.hazlosano.kmp.domain.repository.SleepSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSleepAnalysisUseCaseTest {

    private val fakeRepo = FakeSleepSessionRepository()
    private val useCase = GetSleepAnalysisUseCase(fakeRepo)

    @Test
    fun `returns empty analysis when no sessions`() = runTest {
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(0, result.totalSessions)
        assertEquals(0L, result.totalDurationMillis)
        assertEquals(0f, result.efficiency)
        assertEquals(0L, result.averageDurationMillis)
    }

    @Test
    fun `computes efficiency for one session`() = runTest {
        fakeRepo.sessions.add(
            SleepSession("1", 100L, 500L, SleepSource.PHONE_SENSORS, 1.0f),
        )
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(1, result.totalSessions)
        assertEquals(400L, result.totalDurationMillis)
        assertEquals(0.4f, result.efficiency, 0.001f)
        assertEquals(400L, result.averageDurationMillis)
    }

    @Test
    fun `computes efficiency for multiple sessions`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 100L, 300L, SleepSource.PHONE_SENSORS, 1.0f),
                SleepSession("2", 400L, 600L, SleepSource.PHONE_SENSORS, 1.0f),
            ),
        )
        val result = useCase(from = 0L, to = 1000L)
        assertEquals(2, result.totalSessions)
        assertEquals(400L, result.totalDurationMillis)
        assertEquals(0.4f, result.efficiency, 0.001f)
        assertEquals(200L, result.averageDurationMillis)
    }

    @Test
    fun `filters sessions by range`() = runTest {
        fakeRepo.sessions.addAll(
            listOf(
                SleepSession("1", 100L, 200L, SleepSource.MANUAL, 1.0f),
                SleepSession("2", 500L, 600L, SleepSource.MANUAL, 1.0f),
            ),
        )
        val result = useCase(from = 300L, to = 700L)
        assertEquals(1, result.totalSessions)
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
