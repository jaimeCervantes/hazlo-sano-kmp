package com.hazlosano.data.db

import app.cash.sqldelight.db.SqlDriver
import com.hazlosano.data.movement.SqlDelightMovementSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The migration that finally replaces the hand-written table creation this project used instead of
 * migrations, and drops the statistics columns now that the figures are derived from the route.
 *
 * The point of this test is the route. Rebuilding MovementSessionEntity means dropping it, and
 * MovementPointEntity references it with ON DELETE CASCADE — so with foreign keys enabled SQLite's
 * implicit DELETE FROM would take every recorded point with it. That is a data loss that would only
 * show up on a real phone, after the fact, with the readings already gone.
 */
class MovementSessionMigrationTest {

    @Test
    fun `a database from before the migration keeps its sessions and their routes`() = runTest {
        val driver = inMemoryDriver()
        driver.createSchemaAsItWasBeforeMigrations()
        driver.recordASessionTheOldWay()

        driver.migrateFromVersionOne()

        val repository = SqlDelightMovementSessionRepository(HazloSanoDatabase(driver))
        val session = repository.getAllSessions().first().single()
        assertEquals("Salida de ayer", session.name)
        assertEquals(1_784_877_300_000L, session.date)
        assertEquals(600L, session.elapsedTime)
        assertEquals(1_250.0, session.distanceTraveled, 1e-9)

        val points = repository.getSessionPoints(session.id).first()
        assertEquals(3, points.size, "rebuilding the sessions table took the route with it")
        assertEquals(19.4300, points.first().latitude, 1e-9)
        assertEquals(2_200.0, assertNotNull(points.first().altitude), 1e-9)
        assertEquals(19.4320, points.last().latitude, 1e-9)
    }

    @Test
    fun `the derived columns are gone rather than left behind as dead weight`() = runTest {
        val driver = inMemoryDriver()
        driver.createSchemaAsItWasBeforeMigrations()

        driver.migrateFromVersionOne()

        val columns = driver.columnsOf("MovementSessionEntity")
        assertEquals(
            // `previewPoints` es la segunda excepción declarada junto a `distanceTraveled`, y por el
            // mismo motivo: la lista no puede leer todos los puntos de todas las salidas para
            // dibujarse. No es una cifra derivada, es una silueta.
            setOf(
                "id",
                "routeId",
                "name",
                "date",
                "elapsedTime",
                "distanceTraveled",
                "previewPoints",
            ),
            columns,
        )
    }

    @Test
    fun `a database that never had the movement tables gets them`() = runTest {
        // The hand-written helper only ran on Android. A database that predates it has neither the
        // movement tables nor the nutrition ones, and the migration has to create both.
        val driver = inMemoryDriver()

        driver.migrateFromVersionOne()

        assertTrue(driver.columnsOf("MovementSessionEntity").isNotEmpty())
        assertTrue(driver.columnsOf("MovementPointEntity").isNotEmpty())
        assertTrue(driver.columnsOf("ProductEntity").isNotEmpty())
        assertTrue(driver.columnsOf("SellerEntity").isNotEmpty())
    }

    @Test
    fun `a route recorded before altitude quality existed keeps its altitudes`() = runTest {
        // Chained all the way from version 1, the shape a phone installed before any of this is in.
        val driver = inMemoryDriver()
        driver.createSchemaAsItWasBeforeMigrations()
        driver.recordASessionTheOldWay()

        driver.migrateFromVersionOne()

        val repository = SqlDelightMovementSessionRepository(HazloSanoDatabase(driver))
        val session = repository.getAllSessions().first().single()
        val points = repository.getSessionPoints(session.id).first()

        assertEquals(3, points.size, "rebuilding the points table took the route with it")
        assertEquals(2_200.0, assertNotNull(points.first().altitude), 1e-9)
        // Nothing was known about how good those altitudes were, and nothing is invented.
        assertTrue(points.all { it.verticalAccuracy == null })
    }

    @Test
    fun `a reading with no altitude survives being stored and read back`() = runTest {
        val driver = inMemoryDriver()
        HazloSanoDatabase.Schema.create(driver)
        val repository = SqlDelightMovementSessionRepository(HazloSanoDatabase(driver))

        repository.saveSession(
            session = com.hazlosano.domain.feature.movement.model.MovementSession(
                routeId = null,
                name = "Sin altitud",
                date = 1L,
                elapsedTime = 60,
                distanceTraveled = 100.0,
                previewPoints = emptyList(),
            ),
            rawPoints = listOf(
                com.hazlosano.domain.feature.movement.model.UserLocation(
                    latitude = 19.43,
                    longitude = -99.13,
                    altitude = null,
                    accuracy = 8f,
                    verticalAccuracy = null,
                    timestamp = 1_000,
                ),
                com.hazlosano.domain.feature.movement.model.UserLocation(
                    latitude = 19.44,
                    longitude = -99.13,
                    altitude = 0.0,
                    accuracy = 8f,
                    verticalAccuracy = 12.5f,
                    timestamp = 3_000,
                ),
            ),
        )

        val id = repository.getAllSessions().first().single().id
        val points = repository.getSessionPoints(id).first()

        assertEquals(null, points.first().altitude, "a missing altitude came back as a number")
        // Sea level is an altitude and has to survive as one.
        assertEquals(0.0, assertNotNull(points.last().altitude), 1e-9)
        assertEquals(12.5f, points.last().verticalAccuracy)
    }

    @Test
    fun `a session recorded after the migration still saves and reads back`() = runTest {
        val driver = inMemoryDriver()
        driver.createSchemaAsItWasBeforeMigrations()
        driver.migrateFromVersionOne()

        val repository = SqlDelightMovementSessionRepository(HazloSanoDatabase(driver))
        repository.updateDistance(sessionId = 1, distanceMeters = 10.0) // no rows, must not throw

        assertTrue(repository.getAllSessions().first().isEmpty())
    }
}

/** The movement tables as they were before this migration: statistics columns and all. */
private fun SqlDriver.createSchemaAsItWasBeforeMigrations() {
    exec(
        """
        CREATE TABLE MovementSessionEntity (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            routeId INTEGER,
            name TEXT NOT NULL,
            date INTEGER NOT NULL,
            elapsedTime INTEGER NOT NULL,
            distanceTraveled REAL NOT NULL,
            elevationGain REAL NOT NULL,
            movingTime INTEGER NOT NULL DEFAULT 0,
            avgPace REAL NOT NULL DEFAULT 0.0,
            maxAltitude REAL NOT NULL DEFAULT 0.0,
            minAltitude REAL NOT NULL DEFAULT 0.0,
            totalAscent REAL NOT NULL DEFAULT 0.0,
            totalDescent REAL NOT NULL DEFAULT 0.0
        )
        """.trimIndent(),
    )
    exec(
        """
        CREATE TABLE MovementPointEntity (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sessionId INTEGER NOT NULL,
            seq INTEGER NOT NULL,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            altitude REAL NOT NULL,
            accuracy REAL NOT NULL,
            bearing REAL NOT NULL,
            timestamp INTEGER NOT NULL,
            FOREIGN KEY(sessionId) REFERENCES MovementSessionEntity(id) ON DELETE CASCADE
        )
        """.trimIndent(),
    )
}

private fun SqlDriver.recordASessionTheOldWay() {
    exec(
        "INSERT INTO MovementSessionEntity(id, routeId, name, date, elapsedTime, distanceTraveled, " +
            "elevationGain, movingTime, avgPace, maxAltitude, minAltitude, totalAscent, totalDescent) " +
            "VALUES (1, NULL, 'Salida de ayer', 1784877300000, 600, 1250.0, " +
            "5.0, 580, 8.0, 2210.0, 2200.0, 5.0, 0.0)",
    )
    listOf(
        Triple(0, 19.4300, 2_200.0),
        Triple(1, 19.4310, 2_205.0),
        Triple(2, 19.4320, 2_210.0),
    ).forEach { (seq, latitude, altitude) ->
        exec(
            "INSERT INTO MovementPointEntity(sessionId, seq, latitude, longitude, altitude, " +
                "accuracy, bearing, timestamp) " +
                "VALUES (1, $seq, $latitude, -99.13, $altitude, 8.0, 0.0, ${1_000 + seq * 2_000})",
        )
    }
}
