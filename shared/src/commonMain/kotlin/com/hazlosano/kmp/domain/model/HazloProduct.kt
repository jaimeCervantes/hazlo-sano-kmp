package com.hazlosano.kmp.domain.model

data class HazloProduct(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val isFavorite: Boolean,
    val distanceMeters: Double?,
)
