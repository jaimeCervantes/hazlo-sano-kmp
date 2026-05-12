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
            val date = if (local.hour >= NIGHT_BOUNDARY_HOUR) {
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
        return (sqrt(variance) / MINUTE_MS.toDouble()).toInt()
    }

    private fun computeSleepDebt(nights: List<SleepNight>): Int {
        return nights.sumOf { night ->
            max(0L, TARGET_SLEEP_MS - night.totalDurationMillis)
        }.toInt() / MINUTE_MS.toInt()
    }

    private fun computeTrend(nights: List<SleepNight>): String {
        if (nights.size < 2) return TREND_INSUFFICIENT
        if (nights.size >= MIN_NIGHTS_HALF_TREND) {
            val half = nights.size / 2
            val recent = nights.take(half).map { it.totalDurationMillis }.average()
            val older = nights.takeLast(half).map { it.totalDurationMillis }.average()
            return trendDirection(recent, older)
        }
        val last = nights.first().totalDurationMillis
        val prev = nights[1].totalDurationMillis
        return trendDirection(last.toDouble(), prev.toDouble())
    }

    private fun trendDirection(recent: Double, older: Double): String = when {
        recent > older + TREND_THRESHOLD_MS -> "Mejorando ↑"
        recent < older - TREND_THRESHOLD_MS -> "Empeorando ↓"
        else -> "Estable →"
    }

    companion object {
        const val NIGHT_BOUNDARY_HOUR = 18
        const val TARGET_HOURS = 8
        val TARGET_SLEEP_MS: Long = TARGET_HOURS * 3600_000L
        const val MINUTE_MS: Long = 60_000L
        const val TREND_THRESHOLD_MS = 30 * MINUTE_MS
        const val MIN_NIGHTS_HALF_TREND = 4
        const val TREND_INSUFFICIENT = "—"
    }
}
