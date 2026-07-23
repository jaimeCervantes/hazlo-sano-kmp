package com.hazlosano.domain.feature.movement.model

data class SessionStats(
    val maxAltitude: Double = 0.0,
    val minAltitude: Double = 0.0,
    val totalAscent: Double = 0.0,
    val totalDescent: Double = 0.0,
    val avgSlope: Double = 0.0,
    val maxSlope: Double = 0.0,
    val currentSlope: Double = 0.0,
    val movingTime: Long = 0,
    val vam: Double = 0.0, // Vertical Ascent Velocity (m/h)
    val currentPace: Double = 0.0, // min/km
    val avgPace: Double = 0.0, // min/km
    val altitudeDistribution: Map<Int, Long> = emptyMap()
)
