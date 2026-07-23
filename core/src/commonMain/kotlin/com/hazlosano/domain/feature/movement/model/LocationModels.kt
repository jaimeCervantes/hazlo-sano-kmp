package com.hazlosano.domain.feature.movement.model

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val bearing: Float = 0f,
    val timestamp: Long = 0L
)
