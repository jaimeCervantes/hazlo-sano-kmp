package com.hazlosano.domain.feature.movement.model

import com.hazlosano.domain.geo.haversineMeters

data class Route(
    val id: Long = 0,
    val name: String,
    val distance: Double, // en metros
    /**
     * Desnivel positivo en metros, o `null` cuando **no se puede saber**.
     *
     * Un GPX cuyos puntos no traen elevación no describe una ruta llana: no dice nada sobre su
     * desnivel. Guardar un 0.0 hacía indistinguible "no lo sé" de "es plana", y la lista lo pintaba
     * como «0 m» — una afirmación que la ruta nunca hizo. Es la misma distinción que [WayPoint]
     * hace con la altitud de un punto y `UserLocation` con la de una lectura.
     */
    val elevationGain: Double?,
    val points: List<WayPoint>,
    val fingerprint: String? = null
)

/**
 * Genera una huella digital única basada en estadísticas y puntos clave.
 */
fun Route.calculateFingerprint(): String {
    if (points.isEmpty()) return ""
    val first = points.first()
    val last = points.last()
    // Una firma simple: Distancia-Desnivel-Lat1-Lon1-LatN-LonN
    //
    // Un desnivel desconocido cuenta como 0 **a propósito**: es exactamente lo que se guardaba
    // antes de que la columna fuera nulable, así que las huellas de las rutas ya importadas no
    // cambian y el reconocimiento de duplicados sigue funcionando sobre ellas. Cambiarlo obligaría
    // a recalcular la huella de todo lo guardado para no empezar a ver duplicados donde no los hay.
    return "${distance.toInt()}-${(elevationGain ?: 0.0).toInt()}-" +
            "${first.latitude}-${first.longitude}-" +
            "${last.latitude}-${last.longitude}"
}

/**
 * A point along a route.
 *
 * [altitude] and [timestamp] are null when the file did not carry them, never zero. GPX makes both
 * optional — a route drawn on a map has no times, and plenty of exporters omit elevation — and a
 * point with no elevation is not a point at sea level, the same distinction [UserLocation] makes
 * for a reading with no vertical component.
 */
data class WayPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timestamp: Long? = null,
)

/**
 * Distancia total y desnivel positivo de la ruta.
 *
 * El desnivel es **nulo cuando no hubo con qué medirlo**: si ningún par de puntos consecutivos trae
 * las dos altitudes, no se comparó nada, y devolver 0.0 afirmaría que la ruta es llana. Cero sólo
 * significa cero cuando de verdad se comparó algo y no subió.
 */
fun List<WayPoint>.calculateStats(): Pair<Double, Double?> {
    var totalDistance = 0.0
    var totalElevationGain = 0.0
    var comparedAnyAltitudes = false

    for (i in 0 until size - 1) {
        val p1 = this[i]
        val p2 = this[i + 1]

        // Distancia Haversine
        totalDistance += calculateHaversineDistance(p1, p2)

        // Desnivel positivo. Se cuenta cada ascenso tal cual, sin el umbral que usa el pilar para
        // sus propias lecturas, porque un GPX importado no dice nada sobre la precisión de sus
        // elevaciones contra la que medir.
        val here = p2.altitude
        val there = p1.altitude
        if (here != null && there != null) {
            comparedAnyAltitudes = true
            if (here > there) totalElevationGain += here - there
        }
    }

    return Pair(totalDistance, if (comparedAnyAltitudes) totalElevationGain else null)
}

private fun calculateHaversineDistance(p1: WayPoint, p2: WayPoint): Double {
    return haversineMeters(
        p1.latitude,
        p1.longitude,
        p2.latitude,
        p2.longitude
    )
}
