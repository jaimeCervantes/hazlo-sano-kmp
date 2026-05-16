package com.hazlosano.kmp.data.db

import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.hazlosano.kmp.data.sleep.SleepServiceLocator

actual fun createSqlDriver(): SqlDriver {
    val ctx = SleepServiceLocator.requireContext()
    migrateIfNeeded(ctx)
    return AndroidSqliteDriver(HazloSanoDatabase.Schema, ctx, "hazlosano.db")
}

private fun migrateIfNeeded(context: android.content.Context) {
    val dbFile = context.getDatabasePath("hazlosano.db")
    if (!dbFile.exists()) return

    val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
    val cursor = db.rawQuery(
        "SELECT name FROM sqlite_master WHERE type='table' AND name='ProductEntity'",
        null,
    )
    val hasTable = cursor.count > 0
    cursor.close()

    if (!hasTable) {
        db.execSQL("""
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
        """.trimIndent())

        db.execSQL("""
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
        """.trimIndent())

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_product_category ON ProductEntity(category)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_product_seller ON ProductEntity(sellerId)",
        )
    }
    db.close()
}
