package com.hazlosano.kmp.domain.model

data class SleepAnalysis(
    val totalSessions: Int,
    val totalDurationMillis: Long,
    val efficiency: Float,
    val periodStart: Long,
    val periodEnd: Long,
) {
    val averageDurationMillis: Long
        get() = if (totalSessions > 0) totalDurationMillis / totalSessions else 0L
}
