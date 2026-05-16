package com.hazlosano.kmp.data.sleep

import com.hazlosano.kmp.domain.model.SleepSession

actual fun createSleepDataSource(): SleepDataSource = object : SleepDataSource {
    private val sessions = mutableListOf<SleepSession>()

    override suspend fun getSleepSessions(from: Long, to: Long): List<SleepSession> =
        sessions.filter { it.endTime > from && it.startTime < to }

    override suspend fun saveSleepSession(session: SleepSession) {
        sessions.add(session)
    }
}
