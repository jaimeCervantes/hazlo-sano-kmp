package com.hazlosano.kmp.domain.model

data class SleepAnalysis(
    val totalSessions: Int,
    val totalDurationMillis: Long,
    val efficiency: Float,
    val periodStart: Long,
    val periodEnd: Long,
    val firstSleepStart: Long? = null,
    val lastSleepEnd: Long? = null,
    val averageConfidence: Float = 1.0f,
    val phaseBreakdown: Map<SleepPhase, Long> = emptyMap(),
) {
    val averageDurationMillis: Long
        get() = if (totalSessions > 0) totalDurationMillis / totalSessions else 0L
}
