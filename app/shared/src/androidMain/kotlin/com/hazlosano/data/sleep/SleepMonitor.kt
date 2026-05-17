package com.hazlosano.data.sleep

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

object SleepMonitor {

    private const val TAG = "SleepMonitor"
    private const val WORK_NAME_PERIODIC_REFRESH = "sleep_monitor_refresh"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    @Volatile
    private var started = false
    private var pendingIntent: PendingIntent? = null

    val isStarted: Boolean get() = started

    fun start(context: Context) {
        val ctx = context.applicationContext
        schedulePeriodicRefresh(ctx)
        scope.launch {
            mutex.withLock {
                try {
                    val intent = Intent(ctx, SleepReceiver::class.java)
                    pendingIntent = PendingIntent.getBroadcast(
                        ctx,
                        0,
                        intent,
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )

                    ActivityRecognition.getClient(ctx)
                        .requestSleepSegmentUpdates(
                            pendingIntent!!,
                            SleepSegmentRequest.getDefaultSleepSegmentRequest(),
                        )
                        .addOnSuccessListener {
                            started = true
                            Log.i(TAG, "Sleep monitoring started successfully")
                        }
                        .addOnFailureListener { e ->
                            started = false
                            when (e) {
                                is SecurityException ->
                                    Log.e(TAG, "Permission denied: ACTIVITY_RECOGNITION not granted", e)
                                is ApiException ->
                                    Log.e(TAG, "Google Play Services error: code=${e.statusCode}", e)
                                else ->
                                    Log.e(TAG, "Failed to start sleep monitoring", e)
                            }
                        }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Permission denied", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error starting sleep monitor", e)
                }
            }
        }
    }

    fun stop(context: Context) {
        scope.launch {
            mutex.withLock {
                pendingIntent?.let { pi ->
                    try {
                        ActivityRecognition.getClient(context).removeSleepSegmentUpdates(pi)
                        started = false
                        Log.i(TAG, "Sleep monitoring stopped")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Permission denied while stopping", e)
                    } catch (e: ApiException) {
                        Log.e(TAG, "Play Services error while stopping: code=${e.statusCode}", e)
                    }
                }
                pendingIntent = null
            }
        }
    }

    private fun schedulePeriodicRefresh(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<SleepMonitorWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC_REFRESH,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic sleep monitor refresh", e)
        }
    }
}
