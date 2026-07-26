package com.hazlosano.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * The driver applies `Schema.create` to a new database and the `.sqm` migrations to an existing
 * one. Before those migrations existed, tables added after the first release were created by hand
 * here at startup, because `AndroidSqliteDriver` only creates the schema when the file is absent.
 * That workaround could add whole tables and nothing else, so changing an existing table would have
 * broken an already installed app at runtime with nothing failing at compile time. Migration `1.sqm`
 * now does that work.
 */
fun createSqlDriver(context: Context): SqlDriver =
    AndroidSqliteDriver(HazloSanoDatabase.Schema, context.applicationContext, "hazlosano.db")
