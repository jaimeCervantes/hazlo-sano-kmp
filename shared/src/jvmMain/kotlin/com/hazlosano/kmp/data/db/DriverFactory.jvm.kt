package com.hazlosano.kmp.data.db

import app.cash.sqldelight.db.SqlDriver

actual fun createSqlDriver(): SqlDriver =
    throw UnsupportedOperationException("SQLDelight not supported on JVM in this project — use Android or iOS")
