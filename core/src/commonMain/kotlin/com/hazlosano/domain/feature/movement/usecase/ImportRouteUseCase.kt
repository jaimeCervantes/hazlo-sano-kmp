package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.calculateFingerprint
import com.hazlosano.domain.feature.movement.model.calculateStats
import com.hazlosano.domain.feature.movement.parser.GpxParser
import com.hazlosano.domain.feature.movement.repository.RouteRepository

class ImportRouteUseCase(
    private val repository: RouteRepository,
    private val parser: GpxParser
) {
    sealed class Result {
        data class Success(val route: Route) : Result()
        data class AlreadyExists(val existingRoute: Route, val newRoute: Route) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(data: ByteArray, forceOverwrite: Boolean = false): Result {
        return try {
            val route = parser.parse(data)
            val (totalDistance, totalElevation) = route.points.calculateStats()
            var finalRoute = route.copy(
                distance = totalDistance,
                elevationGain = totalElevation
            )
            val fingerprint = finalRoute.calculateFingerprint()
            finalRoute = finalRoute.copy(fingerprint = fingerprint)

            val existingByName = repository.getRouteByName(finalRoute.name)
            val existingByFingerprint = repository.getRouteByFingerprint(fingerprint)

            val existingRoute = existingByFingerprint ?: existingByName

            if (existingRoute != null && !forceOverwrite) {
                Result.AlreadyExists(existingRoute, finalRoute)
            } else {
                if (forceOverwrite && existingRoute != null) {
                    repository.saveRoute(finalRoute.copy(id = existingRoute.id))
                } else {
                    repository.saveRoute(finalRoute)
                }
                Result.Success(finalRoute)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
