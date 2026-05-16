package com.hazlosano.kmp.domain.model

data class SleepHistory(
    val nights: List<SleepNight>,
    val averageHours: Float,
    val averageEfficiency: Float,
    val consistencyMinutes: Int,
    val sleepWindowHours: Float,
    val sleepDebtMinutes: Int,
    val trendLabel: String,
)
