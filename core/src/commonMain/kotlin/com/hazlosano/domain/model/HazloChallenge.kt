package com.hazlosano.domain.model

data class HazloChallenge(
    val title: String,
    val description: String,
    val progressText: String,
    val progress: Float,
    val imageUrl: String,
)
