package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.RouteProblem
import com.hazlosano.domain.feature.movement.parser.GpxFormat
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.first

/**
 * Writes a stored route back out as GPX, so a route recorded here can be opened by anything else
 * that reads the format — a watch, a bike computer, another phone.
 */
class ExportRouteAsGpxUseCase(
    private val routes: RouteRepository,
) {
    sealed interface Result {
        data class Success(val fileName: String, val gpx: String) : Result

        /** Qué salió mal, sin decidir con qué palabras se cuenta: eso es trabajo de la UI. */
        data class Failed(val problem: RouteProblem) : Result
    }

    suspend operator fun invoke(routeId: Long): Result {
        val route = routes.getRouteWithPoints(routeId).first()
            ?: return Result.Failed(RouteProblem.ROUTE_NOT_FOUND)
        if (route.points.isEmpty()) return Result.Failed(RouteProblem.ROUTE_HAS_NO_POINTS)
        return Result.Success(fileName = fileNameFor(route.name), gpx = GpxFormat.write(route))
    }

    /**
     * The route's own name, reduced to what every filesystem accepts. Keeping the name recognisable
     * matters more than keeping it exact: this is the name the person will look for in their files.
     */
    private fun fileNameFor(name: String): String {
        val safe = name.trim()
            .map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '-' }
            .joinToString("")
            .trim('-')
            .take(MAX_NAME_LENGTH)
        return if (safe.isEmpty()) "ruta.gpx" else "$safe.gpx"
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
    }
}
