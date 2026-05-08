package com.hazlosano.kmp.domain.model

data class SleepContent(
    val heroTitle: String,
    val heroSubtitle: String,
    val heroMetricLabel: String,
    val heroMetricValue: String,
    val heroMetricSupport: String,
    val heroProgress: Float,
    val heroImageUrl: String,
    val weeklyChampions: List<HazloChampion>,
    val activeChallenges: List<HazloChallenge>,
    val productsAndServices: List<HazloProduct>,
)
