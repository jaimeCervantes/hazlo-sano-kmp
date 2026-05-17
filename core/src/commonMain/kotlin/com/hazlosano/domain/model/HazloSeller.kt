package com.hazlosano.domain.model

data class HazloSeller(
    val id: String,
    val name: String,
    val category: String,
    val phone: String,
    val url: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val hasMembership: Boolean = false,
    val hasPaidAds: Boolean = false,
)
