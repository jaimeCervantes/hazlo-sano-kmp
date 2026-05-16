package com.hazlosano.kmp.data.db

import app.cash.sqldelight.db.SqlDriver

object DatabaseProvider {

    private var database: HazloSanoDatabase? = null

    val isInitialized: Boolean get() = database != null

    fun initialize(driver: SqlDriver) {
        if (database == null) {
            database = HazloSanoDatabase(driver)
        }
    }

    fun get(): HazloSanoDatabase =
        database ?: error("DatabaseProvider not initialized. Call initialize(driver) first.")
}
