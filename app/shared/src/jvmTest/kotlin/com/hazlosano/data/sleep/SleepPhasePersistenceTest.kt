package com.hazlosano.data.sleep

import app.cash.sqldelight.db.SqlDriver
import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.data.db.exec
import com.hazlosano.data.db.inMemoryDriver
import com.hazlosano.data.db.inMemoryHazloSanoDatabase
import com.hazlosano.data.db.migrateFromVersionOne
import com.hazlosano.domain.model.SleepPhase
import com.hazlosano.domain.model.SleepSession
import com.hazlosano.domain.model.SleepSource
import com.hazlosano.domain.usecase.GetSleepAnalysisUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The phase of a stretch of the night survives being written down.
 *
 * It did not before. SleepReceiver has always mapped the Android Sleep API status to a SleepPhase,
 * and the table had no column to put it in, so every night read back as UNKNOWN — which is exactly
 * what GetSleepAnalysisUseCase leaves out of its breakdown. The screen showed no phases at all, and
 * nothing failed: the data source simply never passed the phase along.
 */
class SleepPhasePersistenceTest {

    @Test
    fun `a night keeps the phase the detector reported`() = runTest {
        val dataSource = SqlDelightSleepDataSource(inMemoryHazloSanoDatabase())

        dataSource.saveSleepSession(night(start = 10_000, end = 20_000, phase = SleepPhase.DEEP))

        val read = dataSource.getSleepSessions(from = 0, to = 30_000).single()
        assertEquals(SleepPhase.DEEP, read.phase)
    }

    @Test
    fun `every phase the detector can report survives the round trip`() = runTest {
        val dataSource = SqlDelightSleepDataSource(inMemoryHazloSanoDatabase())
        SleepPhase.entries.forEachIndexed { index, phase ->
            val start = index * 10_000L
            dataSource.saveSleepSession(night(start = start, end = start + 5_000, phase = phase))
        }

        val read = dataSource.getSleepSessions(from = 0, to = Long.MAX_VALUE)

        assertEquals(SleepPhase.entries.toSet(), read.map { it.phase }.toSet())
    }

    @Test
    fun `the phase breakdown stops coming back empty`() = runTest {
        // The bug as the screen showed it: the analysis filters UNKNOWN out of the breakdown, and
        // every night was UNKNOWN because the phase was dropped on the way into the database.
        val dataSource = SqlDelightSleepDataSource(inMemoryHazloSanoDatabase())
        dataSource.saveSleepSession(night(start = 0, end = 3_600_000, phase = SleepPhase.LIGHT))
        dataSource.saveSleepSession(
            night(start = 3_600_000, end = 7_200_000, phase = SleepPhase.DEEP),
        )
        val analysis = GetSleepAnalysisUseCase(SleepSessionRepositoryImpl(dataSource))

        val breakdown = analysis(from = 0, to = 7_200_000).phaseBreakdown

        assertEquals(3_600_000L, breakdown[SleepPhase.LIGHT])
        assertEquals(3_600_000L, breakdown[SleepPhase.DEEP])
    }

    @Test
    fun `nights recorded before the phase column keep their hours and admit knowing no phase`() =
        runTest {
            val driver = inMemoryDriver()
            driver.createSleepTableAsItWasBeforeThePhaseColumn()
            driver.recordANightTheOldWay()

            driver.migrateFromVersionOne()

            val read = SqlDelightSleepDataSource(HazloSanoDatabase(driver))
                .getSleepSessions(from = 0, to = Long.MAX_VALUE)
                .single()
            assertEquals(1_784_877_300_000L, read.startTime)
            // The phase those nights were reported with is gone, and nothing invents one for them.
            assertEquals(SleepPhase.UNKNOWN, read.phase)
        }

    @Test
    fun `a database that never had a sleep table still migrates`() = runTest {
        // SleepSessionEntity is created by Schema.create() and by no migration, so a database can
        // reach this migration without it. SQLite has no ALTER TABLE IF EXISTS: without the CREATE
        // that 3.sqm does first, the ALTER would fail and take the whole migration down with it.
        val driver = inMemoryDriver()

        driver.migrateFromVersionOne()

        val dataSource = SqlDelightSleepDataSource(HazloSanoDatabase(driver))
        dataSource.saveSleepSession(night(start = 0, end = 1_000, phase = SleepPhase.REM))
        assertTrue(dataSource.getSleepSessions(from = 0, to = 2_000).isNotEmpty())
    }

    private fun night(start: Long, end: Long, phase: SleepPhase): SleepSession = SleepSession(
        id = "$start-$end",
        startTime = start,
        endTime = end,
        source = SleepSource.PHONE_SENSORS,
        confidence = 1.0f,
        phase = phase,
    )
}

/** The sleep table as it was before it could record a phase. */
private fun SqlDriver.createSleepTableAsItWasBeforeThePhaseColumn() {
    exec(
        """
        CREATE TABLE SleepSessionEntity (
            id TEXT NOT NULL PRIMARY KEY,
            startTime INTEGER NOT NULL,
            endTime INTEGER NOT NULL,
            source TEXT NOT NULL,
            confidence REAL NOT NULL DEFAULT 1.0
        )
        """.trimIndent(),
    )
}

private fun SqlDriver.recordANightTheOldWay() {
    exec(
        "INSERT INTO SleepSessionEntity(id, startTime, endTime, source, confidence) " +
            "VALUES ('anoche', 1784877300000, 1784902500000, 'PHONE_SENSORS', 1.0)",
    )
}
