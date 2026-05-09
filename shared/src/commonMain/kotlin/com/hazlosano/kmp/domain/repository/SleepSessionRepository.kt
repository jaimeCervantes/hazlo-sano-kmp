package com.hazlosano.kmp.domain.repository

import com.hazlosano.kmp.domain.model.SleepSession

interface SleepSessionRepository {
    suspend fun getSleepSessions(from: Long = 0, to: Long = Long.MAX_VALUE): List<SleepSession>
    suspend fun saveSleepSession(session: SleepSession)
}
