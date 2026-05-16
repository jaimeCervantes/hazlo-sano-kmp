package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.model.SleepSource
import com.hazlosano.kmp.domain.repository.SleepSessionRepository
import kotlinx.coroutines.test.runTest
import com.hazlosano.kmp.data.currentEpochMilliseconds
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSleepHistoryUseCaseTest {

    private val fakeRepo = FakeHistorySessionRepository()
    private val useCase = GetSleepHistoryUseCase(fakeRepo)

    private val dayMs = 24 * 3600_000L

    private fun localMidnightToday(): Long {
        val now = Instant.fromEpochMilliseconds(currentEpochMilliseconds())
        val tz = TimeZone.currentSystemDefault()
        return now.toLocalDateTime(tz).date.atStartOfDayIn(tz).toEpochMilliseconds()
    }

    @Test
    fun `empty repo returns zero history`() = runTest {
        val result = useCase(from = 0L, to = 7 * dayMs)
        assertEquals(0, result.nights.size)
        assertEquals(0f, result.averageHours)
        assertEquals(0f, result.averageEfficiency)
        assertEquals(0, result.consistencyMinutes)
        assertEquals(0, result.sleepDebtMinutes)
    }

    @Test
    fun `one session groups into a single night`() = runTest {
        val base = localMidnightToday()
        // 10 PM today (> 18:00 → today's night)
        fakeRepo.sessions.add(sessionAt(base + 22 * 3600_000L, 7))
        val result = useCase(from = 0L, to = base + 2 * dayMs)
        assertEquals(1, result.nights.size)
        assertEquals(7f, result.averageHours, 0.1f)
    }

    @Test
    fun `session after midnight groups with previous night`() = runTest {
        val base = localMidnightToday()
        // 2 AM today (< 18:00 → yesterday's night)
        fakeRepo.sessions.add(sessionAt(base + 2 * 3600_000L, 5))
        val result = useCase(from = 0L, to = base + 2 * dayMs)
        assertEquals(1, result.nights.size)
        assertTrue(result.nights.first().nightKey < base)
    }

    @Test
    fun `two sessions same night are grouped together`() = runTest {
        val base = localMidnightToday()
        // 10 PM today (> 18:00) and 2 AM tomorrow (< 18:00) → same night
        fakeRepo.sessions.add(sessionAt(base + 22 * 3600_000L, 3))
        fakeRepo.sessions.add(sessionAt(base + dayMs + 2 * 3600_000L, 4))
        val result = useCase(from = 0L, to = base + 3 * dayMs)
        assertEquals(1, result.nights.size)
        assertEquals(7 * 3600_000L, result.nights.first().totalDurationMillis)
    }

    @Test
    fun `sessions across different nights are separated`() = runTest {
        val base = localMidnightToday()
        // Night 0: 2 days ago 10 PM
        fakeRepo.sessions.add(sessionAt(base - 2 * dayMs + 22 * 3600_000L, 6))
        // Night 1: yesterday 10 PM
        fakeRepo.sessions.add(sessionAt(base - dayMs + 22 * 3600_000L, 7))
        val result = useCase(from = base - 3 * dayMs, to = base + dayMs)
        assertEquals(2, result.nights.size)
    }

    @Test
    fun `sleep debt accumulates across nights`() = runTest {
        val base = localMidnightToday()
        fakeRepo.sessions.add(sessionAt(base + 22 * 3600_000L, 6)) // 2h deficit
        fakeRepo.sessions.add(sessionAt(base - dayMs + 22 * 3600_000L, 7)) // 1h deficit
        val result = useCase(from = base - 2 * dayMs, to = base + 2 * dayMs)
        assertEquals(180, result.sleepDebtMinutes)
    }

    @Test
    fun `trend improving when recent nights have more sleep`() = runTest {
        val base = localMidnightToday()
        // older nights: 4h
        fakeRepo.sessions.add(sessionAt(base - 4 * dayMs + 22 * 3600_000L, 4))
        fakeRepo.sessions.add(sessionAt(base - 3 * dayMs + 22 * 3600_000L, 4))
        // recent nights: 8h
        fakeRepo.sessions.add(sessionAt(base - 2 * dayMs + 22 * 3600_000L, 8))
        fakeRepo.sessions.add(sessionAt(base - 1 * dayMs + 22 * 3600_000L, 8))
        val result = useCase(from = base - 5 * dayMs, to = base + dayMs)
        assertEquals(4, result.nights.size)
        assertTrue(result.trendLabel.contains("Mejorando"), "trend was: ${result.trendLabel}")
    }

    @Test
    fun `consistency is low when sleep times vary`() = runTest {
        val base = localMidnightToday()
        // vary onset by ~2h
        fakeRepo.sessions.add(sessionAt(base + 22 * 3600_000L, 7))
        fakeRepo.sessions.add(sessionAt(base - dayMs + 23 * 3600_000L, 7))
        fakeRepo.sessions.add(sessionAt(base - 2 * dayMs + 21 * 3600_000L, 7))
        fakeRepo.sessions.add(sessionAt(base - 3 * dayMs + 24 * 3600_000L, 7))
        val result = useCase(from = base - 4 * dayMs, to = base + dayMs)
        assertTrue(result.consistencyMinutes > 30)
    }

    private fun sessionAt(start: Long, durationHours: Int): SleepSession =
        SleepSession("$start-${start + durationHours * 3600_000L}",
            start, start + durationHours * 3600_000L, SleepSource.MANUAL)}

private class FakeHistorySessionRepository : SleepSessionRepository {
    val sessions = mutableListOf<SleepSession>()
    override suspend fun getSleepSessions(from: Long, to: Long): List<SleepSession> =
        sessions.filter { it.endTime > from && it.startTime < to }
    override suspend fun saveSleepSession(session: SleepSession) { sessions.add(session) }
}
