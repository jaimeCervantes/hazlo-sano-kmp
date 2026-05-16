package com.hazlosano.kmp.data

import kotlin.js.Date

actual fun currentEpochMilliseconds(): Long = Date.now().toLong()
