package com.hazlosano.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

fun createSqlDriver(): SqlDriver {
    val driver = NativeSqliteDriver(HazloSanoDatabase.Schema, "hazlosano.db")
    ensureNewTablesExist(driver)
    return driver
}

private fun ensureNewTablesExist(driver: SqlDriver) {
    driver.execute(0, """
        CREATE TABLE IF NOT EXISTS SellerEntity (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            category TEXT NOT NULL,
            phone TEXT NOT NULL,
            url TEXT,
            description TEXT,
            logoUrl TEXT,
            hasMembership INTEGER NOT NULL DEFAULT 0,
            hasPaidAds INTEGER NOT NULL DEFAULT 0
        )
    """.trimIndent(), 0)

    driver.execute(0, """
        CREATE TABLE IF NOT EXISTS ProductEntity (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            price REAL NOT NULL,
            isAvailable INTEGER NOT NULL DEFAULT 1,
            description TEXT,
            category TEXT NOT NULL,
            subCategory TEXT,
            tags TEXT NOT NULL DEFAULT '[]',
            imageUrl TEXT,
            productUrl TEXT,
            sellerId TEXT,
            FOREIGN KEY (sellerId) REFERENCES SellerEntity(id)
        )
    """.trimIndent(), 0)

    driver.execute(
        0,
        "CREATE INDEX IF NOT EXISTS idx_product_category ON ProductEntity(category)",
        0,
    )
    driver.execute(
        0,
        "CREATE INDEX IF NOT EXISTS idx_product_seller ON ProductEntity(sellerId)",
        0,
    )
}
