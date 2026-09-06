package com.hazlosano.data.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import java.sql.Connection
import java.sql.DriverManager

/**
 * In-memory SQLite database for integration tests. SQLite drops an in-memory database when its
 * last connection closes, so a single connection is kept open for the whole test.
 */
internal fun inMemoryHazloSanoDatabase(): HazloSanoDatabase {
    val driver = inMemoryDriver()
    HazloSanoDatabase.Schema.create(driver)
    return HazloSanoDatabase(driver)
}

/** An empty database, for tests that build a schema by hand before letting migrations run. */
internal fun inMemoryDriver(): JdbcDriver {
    val connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    return object : JdbcDriver() {
        override fun getConnection(): Connection = connection
        override fun closeConnection(connection: Connection) = Unit
        override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
        override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
        override fun notifyListeners(vararg queryKeys: String) = Unit
    }
}

/**
 * Migrates all the way to the current head rather than to a version written into the test. Every
 * migration added from now on is exercised against a database that came from version 1 without
 * anyone having to remember to bump a number here.
 */
internal fun SqlDriver.migrateFromVersionOne() {
    HazloSanoDatabase.Schema.migrate(
        driver = this,
        oldVersion = 1,
        newVersion = HazloSanoDatabase.Schema.version,
    )
}

/** Raw DDL/DML, for tests that build a schema by hand before letting migrations run. */
internal fun SqlDriver.exec(sql: String) {
    execute(identifier = null, sql = sql, parameters = 0)
}

/**
 * The columns a table actually has, for asserting what a migration left behind.
 *
 * It lived as a private copy in both `CatalogCacheMigrationTest` and `MovementSessionMigrationTest`.
 * A third migration test needed it, so it moved here instead — the two copies are gone, not
 * duplicated a third time.
 */
internal fun SqlDriver.columnsOf(table: String): Set<String> =
    executeQuery(
        identifier = null,
        sql = "SELECT name FROM pragma_table_info('$table')",
        parameters = 0,
        mapper = { cursor ->
            val names = mutableSetOf<String>()
            while (cursor.next().value) {
                cursor.getString(0)?.let(names::add)
            }
            QueryResult.Value(names.toSet())
        },
    ).value

/** The tables the schema currently holds. */
internal fun SqlDriver.tableNames(): Set<String> =
    executeQuery(
        identifier = null,
        sql = "SELECT name FROM sqlite_master WHERE type = 'table'",
        parameters = 0,
        mapper = { cursor ->
            val names = mutableSetOf<String>()
            while (cursor.next().value) {
                cursor.getString(0)?.let(names::add)
            }
            QueryResult.Value(names.toSet())
        },
    ).value
