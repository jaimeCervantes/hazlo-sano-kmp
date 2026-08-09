package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.WayPoint
import com.hazlosano.domain.feature.movement.model.calculateFingerprint
import com.hazlosano.domain.feature.movement.model.calculateStats
import com.hazlosano.domain.feature.movement.repository.MovementSessionRepository
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.first

/**
 * Keeps an outing you recorded as a route you can follow again, under a name you choose.
 *
 * A session is what happened; a route is what you intend to repeat. They are stored apart because
 * they answer different questions, and because a session belongs to a day while a route outlives
 * the outing that produced it.
 */
class SaveRouteFromSessionUseCase(
    private val routes: RouteRepository,
    private val sessions: MovementSessionRepository,
) {
    sealed interface Result {
        data class Success(val route: Route) : Result
        data class Error(val message: String) : Result
    }

    suspend operator fun invoke(sessionId: Long, name: String): Result {
        val chosen = name.trim()
        if (chosen.isEmpty()) return Result.Error("La ruta necesita un nombre.")

        val points = sessions.getSessionPoints(sessionId).first()
        // Two points is the minimum that describes going anywhere; one is a place, not a route.
        if (points.size < 2) {
            return Result.Error("La sesión no tiene suficientes puntos para guardar una ruta.")
        }

        val wayPoints = points.map {
            WayPoint(
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude,
                timestamp = it.timestamp,
            )
        }
        val (distance, elevationGain) = wayPoints.calculateStats()
        val route = Route(
            name = chosen,
            distance = distance,
            elevationGain = elevationGain,
            points = wayPoints,
        ).let { it.copy(fingerprint = it.calculateFingerprint()) }

        return Result.Success(route.copy(id = routes.saveRoute(route)))
    }
}
