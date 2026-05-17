package com.hazlosano.data.sleep

import com.hazlosano.domain.model.SleepSession
import com.hazlosano.domain.repository.SleepSessionRepository

class SleepSessionRepositoryImpl(
    private val dataSource: SleepDataSource,
) : SleepSessionRepository {

    override suspend fun getSleepSessions(from: Long, to: Long): List<SleepSession> =
        dataSource.getSleepSessions(from, to)

    override suspend fun saveSleepSession(session: SleepSession) {
        dataSource.saveSleepSession(session)
    }
}
