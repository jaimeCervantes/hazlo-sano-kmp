package com.hazlosano.kmp.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.hazlosano.kmp.data.sleep.SleepContextProvider

actual fun createSqlDriver(): SqlDriver =
    AndroidSqliteDriver(HazloSanoDatabase.Schema, SleepContextProvider.applicationContext, "hazlosano.db")
