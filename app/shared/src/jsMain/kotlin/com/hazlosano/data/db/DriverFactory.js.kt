package com.hazlosano.data.db

import app.cash.sqldelight.db.SqlDriver

fun createSqlDriver(): SqlDriver =
    throw UnsupportedOperationException(
        "SQLDelight SQLite driver not available on JS. Product/sleep data uses in-memory storage."
    )
