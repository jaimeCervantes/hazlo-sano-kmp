package com.hazlosano.feature.movement.presentation

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Display labels for movement values, shared by the tracker and the session history so both screens
 * read the same. Kept multiplatform-safe: no platform date/number formatting, and the time zone is a
 * parameter so labels are deterministic under test.
 */
object MovementFormat {

    fun distance(meters: Double): String {
        val safeMeters = meters.coerceAtLeast(0.0)
        if (safeMeters < METERS_PER_KILOMETER) return "${safeMeters.roundToInt()} m"

        val hundredthsOfKm = (safeMeters / METERS_PER_KILOMETER * 100).roundToLong()
        val kilometers = hundredthsOfKm / 100
        val fraction = (hundredthsOfKm % 100).pad2()
        return "$kilometers.$fraction km"
    }

    fun duration(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0L)
        val hours = safeSeconds / SECONDS_PER_HOUR
        val minutes = (safeSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = safeSeconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            "$hours:${minutes.pad2()}:${seconds.pad2()}"
        } else {
            "${minutes.pad2()}:${seconds.pad2()}"
        }
    }

    fun dateTime(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
        val month = MONTH_NAMES[local.monthNumber - 1]
        return "${local.dayOfMonth} $month ${local.year} · ${local.hour.pad2()}:${local.minute.pad2()}"
    }

    private fun Long.pad2(): String = toString().padStart(2, '0')

    private fun Int.pad2(): String = toString().padStart(2, '0')

    private val MONTH_NAMES = listOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic",
    )

    private const val METERS_PER_KILOMETER = 1_000.0
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
}
