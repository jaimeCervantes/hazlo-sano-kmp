package com.hazlosano.kmp.domain.model

data class PillarOverview(
    val pillarType: PillarType,
    val title: String,
    val stat: String,
    val subtitle: String,
    val imageUrl: String,
    val action: PillarAction? = null,
)

enum class PillarAction { TRACKER }
