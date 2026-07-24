package com.hazlosano.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun createSqlDriver(context: Context): SqlDriver {
    val ctx = context.applicationContext
    ensureNewTablesExist(ctx)
    return AndroidSqliteDriver(HazloSanoDatabase.Schema, ctx, "hazlosano.db")
}

private fun ensureNewTablesExist(context: Context) {
    val dbFile = context.getDatabasePath("hazlosano.db")
    if (!dbFile.exists()) return

    val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
    db.beginTransaction()
    try {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='ProductEntity'",
            null,
        )
        val hasTable = cursor.count > 0
        cursor.close()

        if (!hasTable) {
            db.execSQL(
                """
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
                """.trimIndent(),
            )
            db.execSQL(
                """
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
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_product_category ON ProductEntity(category)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_product_seller ON ProductEntity(sellerId)",
            )
        }

        val movementCursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='MovementSessionEntity'",
            null,
        )
        val hasMovementTable = movementCursor.count > 0
        movementCursor.close()

        if (!hasMovementTable) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS MovementSessionEntity (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    routeId INTEGER,
                    name TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    elapsedTime INTEGER NOT NULL,
                    distanceTraveled REAL NOT NULL,
                    elevationGain REAL NOT NULL,
                    movingTime INTEGER NOT NULL DEFAULT 0,
                    avgPace REAL NOT NULL DEFAULT 0.0,
                    maxAltitude REAL NOT NULL DEFAULT 0.0,
                    minAltitude REAL NOT NULL DEFAULT 0.0,
                    totalAscent REAL NOT NULL DEFAULT 0.0,
                    totalDescent REAL NOT NULL DEFAULT 0.0
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS MovementPointEntity (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sessionId INTEGER NOT NULL,
                    seq INTEGER NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    altitude REAL NOT NULL,
                    accuracy REAL NOT NULL,
                    bearing REAL NOT NULL,
                    timestamp INTEGER NOT NULL,
                    FOREIGN KEY(sessionId) REFERENCES MovementSessionEntity(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_movement_session_date ON MovementSessionEntity(date)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_movement_point_session ON MovementPointEntity(sessionId, seq)",
            )
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
        db.close()
    }
}
