package com.hazlosano.data.sleep

import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.domain.model.SleepPhase
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
                    phase = parseSleepPhase(entity.phase),
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
                phase = session.phase.name,
            )
        }

    private fun parseSleepSource(raw: String): SleepSource =
        try {
            SleepSource.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            SleepSource.MANUAL
        }

    /**
     * A phase written by a later version of the app reads as UNKNOWN rather than crashing the night
     * it belongs to. The analysis already leaves UNKNOWN out of the breakdown, so an unrecognised
     * phase is omitted from it instead of being counted as something it is not.
     */
    private fun parseSleepPhase(raw: String): SleepPhase =
        try {
            SleepPhase.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            SleepPhase.UNKNOWN
        }
}
