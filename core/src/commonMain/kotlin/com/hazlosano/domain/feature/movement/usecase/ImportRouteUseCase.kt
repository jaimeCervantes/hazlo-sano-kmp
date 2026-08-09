package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.calculateFingerprint
import com.hazlosano.domain.feature.movement.model.calculateStats
import com.hazlosano.domain.feature.movement.parser.GpxFormat
import com.hazlosano.domain.feature.movement.parser.GpxParser
import com.hazlosano.domain.feature.movement.repository.RouteRepository

/**
 * Reads a GPX file into a stored route.
 *
 * The same track arrives twice more often than it sounds: re-importing the file after editing its
 * name, or receiving from someone else a route already walked. Rather than silently keeping both,
 * an import that recognises the track reports it and lets the caller decide, which is why
 * [Result.AlreadyExists] carries both routes.
 */
class ImportRouteUseCase(
    private val repository: RouteRepository,
    private val parser: GpxParser,
) {
    sealed interface Result {
        data class Success(val route: Route) : Result
        data class AlreadyExists(val existingRoute: Route, val newRoute: Route) : Result
        data class Error(val message: String) : Result
    }

    /**
     * @param fallbackName what to call the route when the track inside the file does not name
     * itself — the file's own name, normally. A route called "Ruta sin nombre" when the file was
     * called something helps nobody find it again.
     */
    suspend operator fun invoke(
        data: ByteArray,
        forceOverwrite: Boolean = false,
        fallbackName: String? = null,
    ): Result = try {
        val parsed = parser.parse(data)
        val (totalDistance, totalElevation) = parsed.points.calculateStats()
        val named = if (parsed.name == GpxFormat.DEFAULT_ROUTE_NAME) {
            fallbackName?.trim()?.takeIf { it.isNotEmpty() } ?: parsed.name
        } else {
            parsed.name
        }
        val route = parsed
            .copy(name = named, distance = totalDistance, elevationGain = totalElevation)
            .let { it.copy(fingerprint = it.calculateFingerprint()) }

        // The fingerprint comes first: it recognises the same track under a different name, which
        // is the case a name comparison cannot see.
        val existing = repository.getRouteByFingerprint(route.fingerprint.orEmpty())
            ?: repository.getRouteByName(route.name)

        when {
            existing != null && !forceOverwrite -> Result.AlreadyExists(existing, route)
            existing != null -> Result.Success(store(route.copy(id = existing.id)))
            else -> Result.Success(store(route))
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "No se pudo leer el archivo GPX.")
    }

    private suspend fun store(route: Route): Route = route.copy(id = repository.saveRoute(route))
}
