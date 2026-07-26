package com.hazlosano.data.movement

import com.hazlosano.data.db.inMemoryHazloSanoDatabase
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Integration test against an in-memory SQLite database. */
class SqlDelightMovementSessionRepositoryTest {

    private val points = listOf(
        UserLocation(latitude = 19.4300, longitude = -99.1300, altitude = 2200.0, accuracy = 5f, bearing = 90f, timestamp = 1000),
        UserLocation(latitude = 19.4310, longitude = -99.1300, altitude = 2205.0, accuracy = 5f, bearing = 90f, timestamp = 2000),
    )

    private fun session(): MovementSession = MovementSession(
        routeId = null,
        name = "Test session",
        date = 123_456L,
        elapsedTime = 60,
        distanceTraveled = 111.0,
        previewPoints = points,
    )

    @Test
    fun savesAndReadsBackASessionWithItsPath() = runTest {
        val repository = SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())

        repository.saveSession(session(), points)

        val savedSessions = repository.getAllSessions().first()
        assertEquals(1, savedSessions.size)
        val saved = savedSessions.first()
        assertEquals("Test session", saved.name)
        assertEquals(111.0, saved.distanceTraveled, 1e-9)
        assertEquals(60L, saved.elapsedTime)

        val savedPoints = repository.getSessionPoints(saved.id).first()
        assertEquals(2, savedPoints.size)
        assertEquals(19.4300, savedPoints.first().latitude, 1e-9)
        assertEquals(2200.0, savedPoints.first().altitude, 1e-9)
        assertEquals(90f, savedPoints.first().bearing)
    }

    @Test
    fun refreshesTheSummaryWithoutTouchingTheRoute() = runTest {
        val repository = SqlDelightMovementSessionRepository(inMemoryHazloSanoDatabase())
        repository.saveSession(session(), points)
        val id = repository.getAllSessions().first().first().id

        repository.updateDistance(id, 98.5)

        val saved = repository.getAllSessions().first().first()
        assertEquals(98.5, saved.distanceTraveled, 1e-9)
        assertEquals(60L, saved.elapsedTime, "refreshing the distance disturbed the rest of the row")
        assertEquals(2, repository.getSessionPoints(id).first().size)
    }
}
