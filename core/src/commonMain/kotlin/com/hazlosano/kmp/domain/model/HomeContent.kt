package com.hazlosano.kmp.domain.model

data class HomeContent(
    val headerTitle: String,
    val headerSubtitle: String,
    val pillars: List<PillarOverview>,
    val champions: List<HazloChampion>,
    val feedPosts: List<FeedPost>,
)
