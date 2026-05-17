package com.hazlosano.domain.model

data class SleepNight(
    val nightKey: Long,
    val sessions: List<SleepSession>,
    val totalDurationMillis: Long,
    val efficiency: Float,
    val firstSleepStart: Long,
    val lastSleepEnd: Long,
)
