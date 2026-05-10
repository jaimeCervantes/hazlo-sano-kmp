package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.SleepAnalysis
import com.hazlosano.kmp.domain.model.SleepPhase
import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.repository.SleepSessionRepository
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
            val clippedStart = max(session.startTime, from)
            val clippedEnd = min(session.endTime, to)
            maxOf(0L, clippedEnd - clippedStart)
        }
        val totalTimeSpanMillis = to - from
        val efficiency = if (totalTimeSpanMillis > 0) {
            (totalDurationMillis.toFloat() / totalTimeSpanMillis.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val mainCluster = findMainSleepCluster(sessions)
        val firstSleepStart = mainCluster.minOf { it.startTime }
        val lastSleepEnd = mainCluster.maxOf { it.endTime }
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
            firstSleepStart = firstSleepStart,
            lastSleepEnd = lastSleepEnd,
            averageConfidence = averageConfidence,
            phaseBreakdown = phaseBreakdown,
        )
    }

    private fun findMainSleepCluster(sessions: List<SleepSession>): List<SleepSession> {
        if (sessions.size <= 1) return sessions

        val sorted = sessions.sortedBy { it.startTime }
        val clusters = mutableListOf<MutableList<SleepSession>>()
        clusters.add(mutableListOf(sorted.first()))

        for (i in 1 until sorted.size) {
            val gap = sorted[i].startTime - sorted[i - 1].endTime
            if (gap < 2 * 60 * 60 * 1000L) {
                clusters.last().add(sorted[i])
            } else {
                clusters.add(mutableListOf(sorted[i]))
            }
        }

        return clusters.maxByOrNull { cluster ->
            cluster.sumOf { it.duration }
        } ?: sessions
    }
}
