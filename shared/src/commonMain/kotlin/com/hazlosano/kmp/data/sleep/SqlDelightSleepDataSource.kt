package com.hazlosano.kmp.data.sleep

import app.cash.sqldelight.db.SqlDriver
import com.hazlosano.kmp.data.db.HazloSanoDatabase
import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.model.SleepSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SqlDelightSleepDataSource(
    driver: SqlDriver,
) : SleepDataSource {

    private val queries = HazloSanoDatabase(driver).sleepSessionQueries

    override suspend fun getSleepSessions(from: Long, to: Long): List<SleepSession> =
        withContext(Dispatchers.Default) {
            queries.selectByRange(from, to).executeAsList().map { entity ->
                SleepSession(
                    id = entity.id,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    source = SleepSource.valueOf(entity.source),
                    confidence = entity.confidence.toFloat(),
                )
            }
        }

    override suspend fun saveSleepSession(session: SleepSession) =
        withContext(Dispatchers.Default) {
            queries.insertOrReplace(
                id = session.id,
                startTime = session.startTime,
                endTime = session.endTime,
                source = session.source.name,
                confidence = session.confidence.toDouble(),
            )
        }
}
