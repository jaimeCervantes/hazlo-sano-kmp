package com.hazlosano.domain.feature.movement.model

import com.hazlosano.domain.geo.haversineMeters

data class Route(
    val id: Long = 0,
    val name: String,
    val distance: Double, // en metros
    val elevationGain: Double, // en metros
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
    return "${distance.toInt()}-${elevationGain.toInt()}-" +
            "${first.latitude}-${first.longitude}-" +
            "${last.latitude}-${last.longitude}"
}

data class WayPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val timestamp: Long = 0
)

/**
 * Calcula las estadísticas de la ruta (distancia total y desnivel positivo).
 */
fun List<WayPoint>.calculateStats(): Pair<Double, Double> {
    var totalDistance = 0.0
    var totalElevationGain = 0.0

    for (i in 0 until size - 1) {
        val p1 = this[i]
        val p2 = this[i + 1]

        // Distancia Haversine
        totalDistance += calculateHaversineDistance(p1, p2)

        // Desnivel positivo
        val elevationDiff = p2.altitude - p1.altitude
        if (elevationDiff > 0) {
            totalElevationGain += elevationDiff
        }
    }

    return Pair(totalDistance, totalElevationGain)
}

private fun calculateHaversineDistance(p1: WayPoint, p2: WayPoint): Double {
    return haversineMeters(
        p1.latitude,
        p1.longitude,
        p2.latitude,
        p2.longitude
    )
}
