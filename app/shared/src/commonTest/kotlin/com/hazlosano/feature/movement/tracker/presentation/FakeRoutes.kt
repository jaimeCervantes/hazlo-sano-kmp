package com.hazlosano.feature.movement.tracker.presentation

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Las rutas guardadas, para los tests del tracker.
 *
 * Vacio por defecto: la mayoria de las pruebas del tracker no hablan de rutas y no deberian tener
 * que decirlo.
 */
internal class FakeRoutes(routes: List<Route> = emptyList()) : RouteRepository {
    private val all = MutableStateFlow(routes)

    override fun getAllRoutes(): Flow<List<Route>> = all.asStateFlow()

    override fun getRouteWithPoints(routeId: Long): Flow<Route?> =
        flowOf(all.value.firstOrNull { it.id == routeId })

    override suspend fun getRouteByName(name: String): Route? = null
    override suspend fun getRouteByFingerprint(fingerprint: String): Route? = null
    override suspend fun saveRoute(route: Route): Long = 0
    override suspend fun renameRoute(routeId: Long, name: String) = Unit
    override suspend fun deleteRoute(routeId: Long) = Unit
}
