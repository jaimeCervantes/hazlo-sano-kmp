package com.hazlosano.kmp.data.sleep

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest
import android.util.Log

object SleepMonitor {

    private const val TAG = "SleepMonitor"
    private var pendingIntent: PendingIntent? = null

    fun start(context: Context) {
        val intent = Intent(context, SleepReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        ActivityRecognition.getClient(context)
            .requestSleepSegmentUpdates(
                pendingIntent!!,
                SleepSegmentRequest.getDefaultSleepSegmentRequest(),
            )
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to request sleep segment updates", e)
            }
    }

    fun stop(context: Context) {
        pendingIntent?.let {
            ActivityRecognition.getClient(context).removeSleepSegmentUpdates(it)
        }
        pendingIntent = null
    }
}
