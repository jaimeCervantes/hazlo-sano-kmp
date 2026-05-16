package com.hazlosano.kmp.data.sleep

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SleepMonitorWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Periodic sleep monitor refresh starting")
        SleepMonitor.start(applicationContext)
        return Result.success()
    }

    companion object {
        private const val TAG = "SleepMonitorWorker"
    }
}
