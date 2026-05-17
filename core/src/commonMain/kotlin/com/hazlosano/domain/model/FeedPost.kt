package com.hazlosano.domain.model

data class FeedPost(
    val userName: String,
    val timeAgo: String,
    val avatarSeed: String,
    val pillarType: PillarType,
    val content: String,
    val imageUrl: String?,
    val likes: Int,
    val comments: Int,
)
