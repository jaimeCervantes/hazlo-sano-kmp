package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.SleepAnalysis
import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.repository.SleepSessionRepository

class GetSleepAnalysisUseCase(
    private val repository: SleepSessionRepository,
) {
    suspend operator fun invoke(from: Long, to: Long): SleepAnalysis {
        val sessions = repository.getSleepSessions(from, to)

        if (sessions.isEmpty()) {
            return SleepAnalysis(
                totalSessions = 0,
                totalDurationMillis = 0L,
                efficiency = 0f,
                periodStart = from,
                periodEnd = to,
            )
        }

        val totalDurationMillis = sessions.sumOf(SleepSession::duration)
        val totalTimeSpanMillis = to - from
        val efficiency = if (totalTimeSpanMillis > 0) {
            (totalDurationMillis.toFloat() / totalTimeSpanMillis.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        return SleepAnalysis(
            totalSessions = sessions.size,
            totalDurationMillis = totalDurationMillis,
            efficiency = efficiency,
            periodStart = from,
            periodEnd = to,
        )
    }
}
