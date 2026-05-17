package com.hazlosano.domain.model

data class SleepSession(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val source: SleepSource,
    val confidence: Float = 1.0f,
    val phase: SleepPhase = SleepPhase.UNKNOWN,
) {
    val duration: Long get() = maxOf(0L, endTime - startTime)
}
