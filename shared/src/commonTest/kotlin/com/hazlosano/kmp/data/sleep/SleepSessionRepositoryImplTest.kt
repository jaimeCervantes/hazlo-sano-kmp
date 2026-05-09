package com.hazlosano.kmp.data.sleep

import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.model.SleepSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SleepSessionRepositoryImplTest {

    @Test
    fun `delegates save and get`() = runTest {
        val dataSource = InMemorySleepDataSource()
        val repo = SleepSessionRepositoryImpl(dataSource)

        val session = SleepSession("1", 100L, 200L, SleepSource.MANUAL)
        repo.saveSleepSession(session)

        val result = repo.getSleepSessions(0L, 1000L)
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }
}

private class InMemorySleepDataSource : SleepDataSource {
    private val list = mutableListOf<SleepSession>()
    override suspend fun getSleepSessions(from: Long, to: Long): List<SleepSession> =
        list.filter { it.endTime > from && it.startTime < to }

    override suspend fun saveSleepSession(session: SleepSession) {
        list.add(session)
    }
}
