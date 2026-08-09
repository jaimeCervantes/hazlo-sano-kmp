package com.hazlosano.data.movement

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.hazlosano.data.currentEpochMilliseconds
import com.hazlosano.data.db.HazloSanoDatabase
import com.hazlosano.data.db.RouteEntity
import com.hazlosano.data.db.RoutePointEntity
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** SQLDelight-backed persistence for routes in the shared [HazloSanoDatabase]. */
class SqlDelightRouteRepository(
    database: HazloSanoDatabase,
    private val now: () -> Long = ::currentEpochMilliseconds,
) : RouteRepository {

    private val queries = database.routeQueries

    /**
     * The list carries no points. Drawing a list of routes must not read every point of every one
     * of them, which is why the row keeps distance and elevation as summaries.
     */
    override fun getAllRoutes(): Flow<List<Route>> =
        queries.selectAllRoutes()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain(points = emptyList()) } }

    override fun getRouteWithPoints(routeId: Long): Flow<Route?> =
        combine(
            queries.selectRouteById(routeId).asFlow().mapToOneOrNull(Dispatchers.Default),
            queries.selectPointsByRoute(routeId).asFlow().mapToList(Dispatchers.Default),
        ) { row, points -> row?.toDomain(points.map { it.toDomain() }) }

    override suspend fun getRouteByName(name: String): Route? =
        withContext(Dispatchers.Default) {
            queries.selectRouteByName(name).executeAsOneOrNull()?.let { it.toDomain(pointsOf(it.id)) }
        }

    override suspend fun getRouteByFingerprint(fingerprint: String): Route? =
        withContext(Dispatchers.Default) {
            // A blank fingerprint belongs to a route with no points, which is not a match for
            // anything: matching on it would collapse every such route into one.
            if (fingerprint.isBlank()) return@withContext null
            queries.selectRouteByFingerprint(fingerprint)
                .executeAsOneOrNull()
                ?.let { it.toDomain(pointsOf(it.id)) }
        }

    /**
     * Writing a route replaces its points wholesale rather than reconciling them. A route is the
     * track it came from; a partially updated one would be a track that never existed.
     */
    override suspend fun saveRoute(route: Route): Long = withContext(Dispatchers.Default) {
        queries.transactionWithResult {
            val id = if (route.id != 0L && exists(route.id)) {
                queries.updateRoute(
                    name = route.name,
                    distance = route.distance,
                    elevationGain = route.elevationGain,
                    fingerprint = route.fingerprint,
                    id = route.id,
                )
                queries.deletePointsByRoute(route.id)
                route.id
            } else {
                queries.insertRoute(
                    name = route.name,
                    distance = route.distance,
                    elevationGain = route.elevationGain,
                    fingerprint = route.fingerprint,
                    createdAt = now(),
                )
                queries.lastInsertedRouteId().executeAsOne()
            }
            route.points.forEachIndexed { index, point ->
                queries.insertRoutePoint(
                    routeId = id,
                    seq = index.toLong(),
                    latitude = point.latitude,
                    longitude = point.longitude,
                    altitude = point.altitude,
                    timestamp = point.timestamp,
                )
            }
            id
        }
    }

    override suspend fun renameRoute(routeId: Long, name: String) {
        withContext(Dispatchers.Default) { queries.renameRoute(name = name, id = routeId) }
    }

    /**
     * The points go first. RoutePointEntity references RouteEntity with ON DELETE CASCADE, but this
     * project enables foreign keys on no driver, so the cascade cannot be relied on to run —
     * deleting only the route would leave its points behind forever.
     */
    override suspend fun deleteRoute(routeId: Long) {
        withContext(Dispatchers.Default) {
            queries.transaction {
                queries.deletePointsByRoute(routeId)
                queries.deleteRoute(routeId)
            }
        }
    }

    private fun exists(routeId: Long): Boolean =
        queries.selectRouteById(routeId).executeAsOneOrNull() != null

    private fun pointsOf(routeId: Long): List<WayPoint> =
        queries.selectPointsByRoute(routeId).executeAsList().map { it.toDomain() }
}

internal fun RouteEntity.toDomain(points: List<WayPoint>): Route = Route(
    id = id,
    name = name,
    distance = distance,
    elevationGain = elevationGain,
    points = points,
    fingerprint = fingerprint,
)

internal fun RoutePointEntity.toDomain(): WayPoint = WayPoint(
    latitude = latitude,
    longitude = longitude,
    altitude = altitude,
    timestamp = timestamp,
)
