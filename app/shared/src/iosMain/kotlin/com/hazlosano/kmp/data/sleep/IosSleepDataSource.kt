package com.hazlosano.kmp.data.sleep

import com.hazlosano.kmp.data.db.DatabaseProvider

actual fun createSleepDataSource(): SleepDataSource =
    SqlDelightSleepDataSource(DatabaseProvider.get())
