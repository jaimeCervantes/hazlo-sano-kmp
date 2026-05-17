package com.hazlosano.data

import kotlin.js.Date

actual fun currentEpochMilliseconds(): Long = Date.now().toLong()
