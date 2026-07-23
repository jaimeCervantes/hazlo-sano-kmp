package com.hazlosano.domain.feature.movement.model

data class NavigationState(
    val traveledPoints: List<UserLocation> = emptyList(),
    val elapsedTime: Long = 0L,
    val distanceTraveled: Double = 0.0,
    val currentSpeed: Double = 0.0,
    val elevationGain: Double = 0.0,
    val isOffRoute: Boolean = false,
    val stats: SessionStats = SessionStats()
)
