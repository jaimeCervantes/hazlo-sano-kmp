package com.hazlosano.data.movement

import com.hazlosano.data.db.inMemoryHazloSanoDatabase
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Integration test against an in-memory SQLite database. */
class SqlDelightMovementSessionRepositoryTest {

    @Test
    fun savesAndReadsBackASessionWithItsPath() = runTest {
        val repository = SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())
        val points = listOf(
            UserLocation(latitude = 19.4300, longitude = -99.1300, altitude = 2200.0, accuracy = 5f, bearing = 90f, timestamp = 1000),
            UserLocation(latitude = 19.4310, longitude = -99.1300, altitude = 2205.0, accuracy = 5f, bearing = 90f, timestamp = 2000),
        )
        val session = MovementSession(
            routeId = null,
            name = "Test session",
            date = 123_456L,
            elapsedTime = 60,
            distanceTraveled = 111.0,
            elevationGain = 5.0,
            previewPoints = points,
            stats = SessionStats(totalAscent = 5.0, avgPace = 9.0, movingTime = 55),
        )

        repository.saveSession(session, points)

        val savedSessions = repository.getAllSessions().first()
        assertEquals(1, savedSessions.size)
        val saved = savedSessions.first()
        assertEquals("Test session", saved.name)
        assertEquals(111.0, saved.distanceTraveled, 1e-9)
        assertEquals(60L, saved.elapsedTime)
        assertEquals(5.0, saved.stats.totalAscent, 1e-9)

        val savedPoints = repository.getSessionPoints(saved.id).first()
        assertEquals(2, savedPoints.size)
        assertEquals(19.4300, savedPoints.first().latitude, 1e-9)
        assertEquals(90f, savedPoints.first().bearing)
    }
}
