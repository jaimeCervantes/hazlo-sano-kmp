package com.hazlosano.feature.movement.routes.presentation

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.model.UserLocation

/**
 * Qué enseña la pantalla de una ruta.
 *
 * Tipo cerrado y sin texto redactado: los rótulos los pone la pantalla. Lo que sí viaja ya
 * convertido son las **cifras**, que es formato y no copia — la misma distinción que `AGENTS.md`
 * hace con los nombres de mes de `MovementFormat`.
 */
sealed interface RouteDetailUiState {

    data object Loading : RouteDetailUiState

    /** La ruta ya no está: se borró desde la lista mientras esta pantalla la miraba. */
    data object Missing : RouteDetailUiState

    data class Detail(
        val name: String,
        val distanceMeters: Double,
        /**
         * Metros de desnivel, o `null` cuando **no se puede saber**.
         *
         * Un GPX sin elevaciones no describe una ruta llana: no dice nada sobre el desnivel.
         * Guardar un 0.0 y pintarlo sería afirmar que es plana, que es una afirmación distinta y
         * probablemente falsa. La misma distinción que hace `MovementFormat.elevation`.
         */
        val elevationGainMeters: Double?,
        val pointCount: Int,
        val path: List<UserLocation>,
    ) : RouteDetailUiState {
        /** Un punto es un lugar, no un trazado: hacen falta dos para dibujar una línea. */
        val hasPath: Boolean get() = path.size >= 2
    }
}

/**
 * De la ruta guardada a lo que la pantalla necesita.
 *
 * Función pura y fuera del ViewModel para poder comprobar las reglas —el desnivel desconocido, el
 * trazado que no se puede dibujar— sin levantar nada.
 */
fun routeDetail(route: Route?): RouteDetailUiState {
    if (route == null) return RouteDetailUiState.Missing

    return RouteDetailUiState.Detail(
        name = route.name,
        distanceMeters = route.distance,
        elevationGainMeters = route.elevationGainOrUnknown(),
        pointCount = route.points.size,
        path = route.points.map { point ->
            UserLocation(
                latitude = point.latitude,
                longitude = point.longitude,
                altitude = point.altitude,
                timestamp = point.timestamp ?: 0L,
            )
        },
    )
}

/**
 * El desnivel sólo significa algo si algún punto traía altitud.
 *
 * `calculateStats` acumula ceros cuando ningún punto la trae, y ese cero acaba guardado en la fila
 * de la ruta indistinguible de un llano de verdad. Aquí se recupera la diferencia mirando los
 * puntos, que es donde está la respuesta.
 */
private fun Route.elevationGainOrUnknown(): Double? =
    if (points.any { it.altitude != null }) elevationGain else null
