package com.hazlosano.data.sleep

import com.hazlosano.data.db.DatabaseProvider

actual fun createSleepDataSource(): SleepDataSource =
    SqlDelightSleepDataSource(DatabaseProvider.get())
