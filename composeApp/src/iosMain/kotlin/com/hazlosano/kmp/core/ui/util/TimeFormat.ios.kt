package com.hazlosano.kmp.core.ui.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual fun formatClockTime(millis: Long): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "hh:mm a"
    return formatter.stringFromDate(NSDate(millis / 1000.0))
}
