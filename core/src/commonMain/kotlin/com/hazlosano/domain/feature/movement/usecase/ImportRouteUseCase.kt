package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.calculateFingerprint
import com.hazlosano.domain.feature.movement.model.calculateStats
import com.hazlosano.domain.feature.movement.parser.GpxFormat
import com.hazlosano.domain.feature.movement.parser.GpxParser
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    /**
     * Dónde se lee y se mide el archivo.
     *
     * Entra por constructor en vez de fijarse dentro porque un test con reloj virtual no puede
     * seguir a `Dispatchers.Default`: el trabajo se le escapa del scheduler y las aserciones corren
     * antes de que termine. Que sea inyectable es lo que hace que el cambio de hilo sea comprobable
     * en vez de una promesa.
     */
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
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
        // Leer un GPX y medirlo es trabajo de CPU sobre un archivo que puede traer miles de puntos,
        // y hasta ahora corría en el hilo de quien llamara — que en Android es el principal, porque
        // `viewModelScope` es `Dispatchers.Main`. Lo declara el caso de uso y no cada llamante: es
        // el que sabe que el trabajo pesa.
        //
        // `Dispatchers.Default` es API común, así que vale en los cinco targets. En web, donde no
        // hay más que un hilo, no hace nada y tampoco estorba.
        val route = withContext(workDispatcher) {
            val parsed = parser.parse(data)
            val (totalDistance, totalElevation) = parsed.points.calculateStats()
            val named = if (parsed.name == GpxFormat.DEFAULT_ROUTE_NAME) {
                fallbackName?.trim()?.takeIf { it.isNotEmpty() } ?: parsed.name
            } else {
                parsed.name
            }
            parsed
                .copy(name = named, distance = totalDistance, elevationGain = totalElevation)
                .let { it.copy(fingerprint = it.calculateFingerprint()) }
        }

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
