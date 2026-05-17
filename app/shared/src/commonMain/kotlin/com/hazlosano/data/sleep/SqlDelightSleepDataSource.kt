package com.hazlosano.data.sleep

import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.domain.model.SleepSession
import com.hazlosano.domain.model.SleepSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SqlDelightSleepDataSource(
    database: HazloSanoDatabase,
) : SleepDataSource {

    private val queries = database.sleepSessionQueries

    override suspend fun getSleepSessions(from: Long, to: Long): List<SleepSession> =
        withContext(Dispatchers.Default) {
            queries.selectByRange(from, to).executeAsList().map { entity ->
                SleepSession(
                    id = entity.id,
                    startTime = entity.startTime,
                    endTime = entity.endTime,
                    source = parseSleepSource(entity.source),
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

    private fun parseSleepSource(raw: String): SleepSource =
        try {
            SleepSource.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            SleepSource.MANUAL
        }
}
