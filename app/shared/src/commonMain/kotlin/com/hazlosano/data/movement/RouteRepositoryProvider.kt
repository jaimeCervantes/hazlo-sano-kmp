package com.hazlosano.data.movement

import com.hazlosano.data.db.DatabaseProvider
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Route persistence: SQLDelight when the shared database is initialized (Android today), otherwise
 * a no-op so the movement screens still render on the other targets.
 */
internal fun routeRepository(): RouteRepository =
    if (DatabaseProvider.isInitialized) {
        SqlDelightRouteRepository(DatabaseProvider.get())
    } else {
        NoOpRouteRepository
    }

/** Used on platforms without an initialized database. */
object NoOpRouteRepository : RouteRepository {
    override fun getAllRoutes(): Flow<List<Route>> = flowOf(emptyList())
    override fun getRouteWithPoints(routeId: Long): Flow<Route?> = flowOf(null)
    override suspend fun getRouteByName(name: String): Route? = null
    override suspend fun getRouteByFingerprint(fingerprint: String): Route? = null
    override suspend fun saveRoute(route: Route): Long = 0L
    override suspend fun renameRoute(routeId: Long, name: String) = Unit
    override suspend fun deleteRoute(routeId: Long) = Unit
}
