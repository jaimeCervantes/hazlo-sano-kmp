package com.hazlosano.kmp.core.ui.util

actual fun formatClockTime(millis: Long): String {
    val date = kotlin.js.Date(millis.toDouble())
    val hours = date.getHours().toInt()
    val minutes = date.getMinutes().toInt()
    val ampm = if (hours < 12) "AM" else "PM"
    val h12 = if (hours % 12 == 0) 12 else hours % 12
    val minStr = if (minutes < 10) "0$minutes" else "$minutes"
    return "$h12:$minStr $ampm"
}
