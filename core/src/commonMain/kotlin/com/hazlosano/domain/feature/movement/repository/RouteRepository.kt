package com.hazlosano.domain.feature.movement.repository

import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    // Routes
    fun getAllRoutes(): Flow<List<Route>>
    fun getRouteWithPoints(routeId: Long): Flow<Route?>
    suspend fun getRouteByName(name: String): Route?
    suspend fun getRouteByFingerprint(fingerprint: String): Route?
    suspend fun saveRoute(route: Route)
    suspend fun deleteRoute(routeId: Long)

    // Sessions
    fun getAllSessions(): Flow<List<MovementSession>>
    fun getSessionById(sessionId: Long): Flow<MovementSession?>
    fun getSessionPoints(sessionId: Long): Flow<List<UserLocation>>
    suspend fun saveSession(session: MovementSession, rawPoints: List<UserLocation>)
}
