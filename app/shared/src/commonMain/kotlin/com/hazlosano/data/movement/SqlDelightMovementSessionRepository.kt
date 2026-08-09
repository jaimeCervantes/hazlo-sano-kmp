package com.hazlosano.data.movement

import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** SQLDelight-backed persistence for movement sessions in the shared [HazloSanoDatabase]. */
class SqlDelightMovementSessionRepository(
    database: HazloSanoDatabase,
) : MovementSessionRepository {

    private val queries = database.movementSessionQueries

    override fun getAllSessions(): Flow<List<MovementSession>> =
        queries.selectAllSessions()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>> =
        queries.selectPointsBySession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>) {
        withContext(Dispatchers.Default) {
            queries.transaction {
                queries.insertSession(
                    routeId = session.routeId,
                    name = session.name,
                    date = session.date,
                    elapsedTime = session.elapsedTime,
                    distanceTraveled = session.distanceTraveled,
                )
                val sessionId = queries.lastInsertedSessionId().executeAsOne()
                rawPoints.forEachIndexed { index, point ->
                    queries.insertPoint(
                        sessionId = sessionId,
                        seq = index.toLong(),
                        latitude = point.latitude,
                        longitude = point.longitude,
                        altitude = point.altitude,
                        accuracy = point.accuracy.toDouble(),
                        verticalAccuracy = point.verticalAccuracy?.toDouble(),
                        bearing = point.bearing.toDouble(),
                        timestamp = point.timestamp,
                    )
                }
            }
        }
    }

    override suspend fun updateDistance(sessionId: Long, distanceMeters: Double) {
        withContext(Dispatchers.Default) {
            queries.updateDistance(distanceTraveled = distanceMeters, id = sessionId)
        }
    }
}
