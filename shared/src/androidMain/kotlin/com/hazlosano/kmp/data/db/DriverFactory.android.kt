package com.hazlosano.kmp.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.hazlosano.kmp.data.sleep.SleepServiceLocator

actual fun createSqlDriver(): SqlDriver =
    AndroidSqliteDriver(HazloSanoDatabase.Schema, SleepServiceLocator.requireContext(), "hazlosano.db")
