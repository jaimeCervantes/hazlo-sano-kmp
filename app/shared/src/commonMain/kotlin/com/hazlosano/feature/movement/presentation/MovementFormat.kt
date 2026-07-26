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

    fun distance(meters: Double?): String {
        if (meters == null) return NO_VALUE
        val safeMeters = meters.coerceAtLeast(0.0)
        if (safeMeters < METERS_PER_KILOMETER) return "${safeMeters.roundToInt()} m"

        val hundredthsOfKm = (safeMeters / METERS_PER_KILOMETER * 100).roundToLong()
        val kilometers = hundredthsOfKm / 100
        val fraction = (hundredthsOfKm % 100).pad2()
        return "$kilometers.$fraction km"
    }

    fun duration(totalSeconds: Long?): String {
        if (totalSeconds == null) return NO_VALUE
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

    /** Average pace as `mm:ss /km`; a session without distance or time has no meaningful pace. */
    fun pace(minutesPerKilometer: Double?): String {
        if (minutesPerKilometer == null) return NO_VALUE
        if (minutesPerKilometer <= 0.0 || !minutesPerKilometer.isFinite()) return NO_VALUE

        val totalSeconds = (minutesPerKilometer * SECONDS_PER_MINUTE).roundToLong()
        val minutes = totalSeconds / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return "$minutes:${seconds.pad2()} /km"
    }

    /**
     * Metres, or "—" when the figure was never measured. Null is not zero here: a session whose
     * readings carried no altitude has no climb, and reporting zero would claim flat ground.
     */
    fun elevation(meters: Double?): String =
        if (meters == null) NO_VALUE else "${meters.roundToInt()} m"

    /** One decimal place without platform number formatting, for values that round badly to whole. */
    fun oneDecimal(value: Double): String {
        val tenths = (value.coerceAtLeast(0.0) * 10).roundToLong()
        return "${tenths / 10}.${tenths % 10}"
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

    private const val NO_VALUE = "—"
    private const val METERS_PER_KILOMETER = 1_000.0
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
}
