package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.SleepAnalysis
import com.hazlosano.domain.model.SleepPhase
import com.hazlosano.domain.model.SleepSession
import com.hazlosano.domain.repository.SleepSessionRepository
import kotlin.math.max
import kotlin.math.min

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

        val totalDurationMillis = sessions.sumOf { session ->
            val cs = max(session.startTime, from)
            val ce = min(session.endTime, to)
            maxOf(0L, ce - cs)
        }

        val firstSleepStart = sessions.minOf { it.startTime }
        val lastSleepEnd = sessions.maxOf { it.endTime }
        val sleepPeriodStart = max(firstSleepStart, from)
        val sleepPeriodEnd = min(lastSleepEnd, to)
        val sleepPeriodMillis = sleepPeriodEnd - sleepPeriodStart

        val efficiency = if (sleepPeriodMillis > 0) {
            (totalDurationMillis.toFloat() / sleepPeriodMillis.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val averageConfidence = sessions.map { it.confidence }.average().toFloat()
        val phaseBreakdown = sessions
            .filter { it.phase != SleepPhase.UNKNOWN && it.phase != SleepPhase.AWAKE }
            .groupBy { it.phase }
            .mapValues { (_, phaseSessions) ->
                phaseSessions.sumOf { session ->
                    val cs = max(session.startTime, from)
                    val ce = min(session.endTime, to)
                    maxOf(0L, ce - cs)
                }
            }

        return SleepAnalysis(
            totalSessions = sessions.size,
            totalDurationMillis = totalDurationMillis,
            efficiency = efficiency,
            periodStart = from,
            periodEnd = to,
            firstSleepStart = sleepPeriodStart,
            lastSleepEnd = sleepPeriodEnd,
            averageConfidence = averageConfidence,
            phaseBreakdown = phaseBreakdown,
        )
    }
}
