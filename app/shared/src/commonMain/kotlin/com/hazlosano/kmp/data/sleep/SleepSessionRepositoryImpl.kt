package com.hazlosano.kmp.data.sleep

import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.repository.SleepSessionRepository

class SleepSessionRepositoryImpl(
    private val dataSource: SleepDataSource,
) : SleepSessionRepository {

    override suspend fun getSleepSessions(from: Long, to: Long): List<SleepSession> =
        dataSource.getSleepSessions(from, to)

    override suspend fun saveSleepSession(session: SleepSession) {
        dataSource.saveSleepSession(session)
    }
}
