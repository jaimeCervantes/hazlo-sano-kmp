package com.hazlosano.kmp.data.sleep

import android.content.Context
import com.hazlosano.kmp.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SleepServiceLocator {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun isInitialized(): Boolean = appContext != null

    fun requireContext(): Context =
        appContext ?: throw IllegalStateException(
            "SleepServiceLocator not initialized. Call initialize(context) first.",
        )

    suspend fun getSleepDataSource(): SleepDataSource =
        withContext(Dispatchers.Default) {
            SqlDelightSleepDataSource(DatabaseProvider.get())
        }
}
