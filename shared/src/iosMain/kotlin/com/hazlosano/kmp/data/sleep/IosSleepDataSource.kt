package com.hazlosano.kmp.data.sleep

import com.hazlosano.kmp.data.db.createSqlDriver

actual fun createSleepDataSource(): SleepDataSource =
    SqlDelightSleepDataSource(createSqlDriver())
