package com.hazlosano.kmp.data.sleep

import android.content.Context
import com.hazlosano.kmp.data.db.createSqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object SleepServiceLocator {

    private val mutex = Mutex()
    private var appContext: Context? = null
    private var dataSource: SleepDataSource? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun isInitialized(): Boolean = appContext != null

    fun requireContext(): Context =
        appContext ?: throw IllegalStateException(
            "SleepServiceLocator not initialized. Call initialize(context) first.",
        )

    suspend fun getSleepDataSource(): SleepDataSource = mutex.withLock {
        dataSource ?: withContext(Dispatchers.Default) {
            SqlDelightSleepDataSource(createSqlDriver())
        }.also { dataSource = it }
    }
}
