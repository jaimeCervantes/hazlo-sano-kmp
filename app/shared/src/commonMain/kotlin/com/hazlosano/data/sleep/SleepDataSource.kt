package com.hazlosano.data.sleep

import com.hazlosano.domain.model.SleepSession

interface SleepDataSource {
    suspend fun getSleepSessions(from: Long = 0, to: Long = Long.MAX_VALUE): List<SleepSession>
    suspend fun saveSleepSession(session: SleepSession)
}

expect fun createSleepDataSource(): SleepDataSource
