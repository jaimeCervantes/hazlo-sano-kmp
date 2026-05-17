package com.hazlosano.data.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

fun createSqlDriver(): SqlDriver {
    val dbDir = File(System.getProperty("user.home"), ".hazlosano").also { it.mkdirs() }
    val dbFile = File(dbDir, "hazlosano.db")
    val url = "jdbc:sqlite:${dbFile.absolutePath}"

    return object : JdbcDriver() {
        override fun getConnection(): Connection = DriverManager.getConnection(url)
        override fun closeConnection(connection: Connection) = connection.close()
        override fun addListener(vararg queryKeys: String, listener: Query.Listener) {}
        override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {}
        override fun notifyListeners(vararg queryKeys: String) {}
    }.also { driver ->
        HazloSanoDatabase.Schema.create(driver)
    }
}
