package com.hazlosano.domain.feature.movement.model

data class MovementSession(
    val id: Long = 0,
    val routeId: Long?,
    val name: String,
    val date: Long,
    val elapsedTime: Long, // in seconds
    val distanceTraveled: Double, // in meters
    val elevationGain: Double, // in meters
    val previewPoints: List<UserLocation>,
    val stats: SessionStats
) {
    val avgSpeedKmH: Double
        get() = if (elapsedTime > 0) (distanceTraveled / elapsedTime.toDouble()) * 3.6 else 0.0

    val avgPaceMinKm: Double
        get() = if (distanceTraveled > 0) (elapsedTime / 60.0) / (distanceTraveled / 1000.0) else 0.0
}
