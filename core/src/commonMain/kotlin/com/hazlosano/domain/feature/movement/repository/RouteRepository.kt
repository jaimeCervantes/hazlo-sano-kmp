package com.hazlosano.domain.feature.movement.repository

import com.hazlosano.domain.feature.movement.model.Route
import kotlinx.coroutines.flow.Flow

/**
 * Routes: the tracks you intend to follow, imported from a GPX file or kept from an outing you
 * recorded. Sessions — what actually happened, reading by reading — belong to
 * [MovementSessionRepository]; this interface used to declare both, which made every implementation
 * owe methods it had no business owning.
 */
interface RouteRepository {
    fun getAllRoutes(): Flow<List<Route>>
    fun getRouteWithPoints(routeId: Long): Flow<Route?>
    suspend fun getRouteByName(name: String): Route?
    suspend fun getRouteByFingerprint(fingerprint: String): Route?

    /** @return the id the route was stored under, whether it was inserted or replaced. */
    suspend fun saveRoute(route: Route): Long
    suspend fun renameRoute(routeId: Long, name: String)
    suspend fun deleteRoute(routeId: Long)
}
