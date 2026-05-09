package com.hazlosano.kmp.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual fun createSqlDriver(): SqlDriver =
    NativeSqliteDriver(HazloSanoDatabase.Schema, "hazlosano.db")
