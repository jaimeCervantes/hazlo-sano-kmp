package com.hazlosano.kmp.domain.model

data class SleepSession(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val source: SleepSource,
    val confidence: Float = 1.0f,
) {
    val duration: Long get() = endTime - startTime
}
