package com.hazlosano.kmp.core.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatClockTime(millis: Long): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}
