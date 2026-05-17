package com.hazlosano.domain.model

data class HazloProduct(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val isFavorite: Boolean = false,
    val distanceMeters: Double? = null,
    val category: String = "",
    val subCategory: String? = null,
    val tags: List<String> = emptyList(),
    val productUrl: String? = null,
    val sellerId: String? = null,
    val isAvailable: Boolean = true,
)
