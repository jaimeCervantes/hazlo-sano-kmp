package com.hazlosano.core.ui.util

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

fun formatClockTime(epochMillis: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour
    val minute = local.minute
    val ampm = if (hour < 12) "AM" else "PM"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val minStr = if (minute < 10) "0$minute" else "$minute"
    return "$h12:$minStr $ampm"
}

fun formatDate(epochMillis: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val dayName = shortDayName(local.dayOfWeek)
    return "$dayName ${local.dayOfMonth}"
}

fun formatWakeDate(nightKeyEpoch: Long): String {
    val wakeEpoch = nightKeyEpoch + 24 * 3600_000L
    return formatDate(wakeEpoch)
}

private val DAY_NAMES = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

private fun shortDayName(dayOfWeek: DayOfWeek): String {
    return DAY_NAMES[dayOfWeek.ordinal]
}
