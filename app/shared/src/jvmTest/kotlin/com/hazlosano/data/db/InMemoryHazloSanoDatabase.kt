package com.hazlosano.data.db

import app.cash.sqldelight.Query
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
