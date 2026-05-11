package com.hazlosano.kmp.domain.usecase

import com.hazlosano.kmp.domain.model.SleepHistory
import com.hazlosano.kmp.domain.model.SleepNight
import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.repository.SleepSessionRepository
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class GetSleepHistoryUseCase(
    private val repository: SleepSessionRepository,
) {
    suspend operator fun invoke(from: Long, to: Long): SleepHistory {
        val sessions = repository.getSleepSessions(from, to)

        val nights = groupByNight(sessions).map { (nightKey, nightSessions) ->
            val totalDuration = nightSessions.sumOf { it.duration }
            val firstStart = nightSessions.minOf { it.startTime }
            val lastEnd = nightSessions.maxOf { it.endTime }
            val period = lastEnd - firstStart
            val efficiency = if (period > 0) {
                (totalDuration.toFloat() / period.toFloat()).coerceIn(0f, 1f)
            } else 0f

            SleepNight(
                nightKey = nightKey,
                sessions = nightSessions.sortedBy { it.startTime },
                totalDurationMillis = totalDuration,
                efficiency = efficiency,
                firstSleepStart = firstStart,
                lastSleepEnd = lastEnd,
            )
        }.sortedByDescending { it.nightKey }

        return SleepHistory(
            nights = nights,
            averageHours = if (nights.isNotEmpty()) {
                nights.map { it.totalDurationMillis }.average().toFloat() / 3600_000f
            } else 0f,
            averageEfficiency = if (nights.isNotEmpty()) {
                nights.map { it.efficiency }.average().toFloat()
            } else 0f,
            consistencyMinutes = computeConsistency(nights),
            sleepWindowHours = if (nights.isNotEmpty()) {
                nights.map { (it.lastSleepEnd - it.firstSleepStart).toFloat() / 3600_000f }
                    .average().toFloat()
            } else 0f,
            sleepDebtMinutes = computeSleepDebt(nights),
            trendLabel = computeTrend(nights),
        )
    }

    private fun groupByNight(sessions: List<SleepSession>): Map<Long, List<SleepSession>> {
        val tz = TimeZone.currentSystemDefault()
        return sessions.groupBy { session ->
            val instant = Instant.fromEpochMilliseconds(session.startTime)
            val local = instant.toLocalDateTime(tz)
            val date = if (local.hour >= 18) {
                local.date
            } else {
                local.date.minus(1, DateTimeUnit.DAY)
            }
            date.atStartOfDayIn(tz).toEpochMilliseconds()
        }
    }

    private fun computeConsistency(nights: List<SleepNight>): Int {
        if (nights.size < 2) return 0
        val onsets = nights.map { it.firstSleepStart.toDouble() }
        val mean = onsets.average()
        val variance = onsets.map { (it - mean) * (it - mean) }.average()
        return (sqrt(variance) / 60_000.0).toInt()
    }

    private fun computeSleepDebt(nights: List<SleepNight>): Int {
        val targetMs = 8 * 3600_000L
        return nights.sumOf { night ->
            max(0L, targetMs - night.totalDurationMillis)
        }.toInt() / 60_000
    }

    private fun computeTrend(nights: List<SleepNight>): String {
        if (nights.size < 2) return "—"
        if (nights.size >= 4) {
            val half = nights.size / 2
            val recent = nights.take(half).map { it.totalDurationMillis }.average()
            val older = nights.takeLast(half).map { it.totalDurationMillis }.average()
            return when {
                recent > older + 30 * 60_000L -> "Mejorando ↑"
                recent < older - 30 * 60_000L -> "Empeorando ↓"
                else -> "Estable →"
            }
        }
        val last = nights.first().totalDurationMillis
        val prev = nights[1].totalDurationMillis
        return when {
            last > prev + 30 * 60_000L -> "Mejorando ↑"
            last < prev - 30 * 60_000L -> "Empeorando ↓"
            else -> "Estable →"
        }
    }
}
