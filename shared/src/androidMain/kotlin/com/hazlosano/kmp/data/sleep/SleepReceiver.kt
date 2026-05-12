package com.hazlosano.kmp.data.sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.SleepSegmentEvent
import com.hazlosano.kmp.domain.model.SleepPhase
import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.model.SleepSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SleepReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SleepReceiver"
        private const val STATUS_AWAKE = 1
        private const val STATUS_ASLEEP = 2
        private const val STATUS_LIGHT = 3
        private const val STATUS_DEEP = 4
        private const val STATUS_REM = 5
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called")

        if (!SleepSegmentEvent.hasEvents(intent)) {
            Log.d(TAG, "No sleep segment events in intent")
            return
        }

        if (!SleepServiceLocator.isInitialized()) {
            SleepServiceLocator.initialize(context)
            Log.d(TAG, "ServiceLocator initialized from receiver")
        }

        val events: List<SleepSegmentEvent> = SleepSegmentEvent.extractEvents(intent)
        Log.d(TAG, "Extracted ${events.size} sleep segment events")
        if (events.isEmpty()) return

        val pendingResult = goAsync()
        val validEvents = events.filter { event ->
            val valid = event.endTimeMillis > 0 && event.endTimeMillis < Long.MAX_VALUE
            if (!valid) {
                Log.w(TAG, "Skipping event with invalid endTimeMillis=${event.endTimeMillis}")
            }
            valid
        }

        if (validEvents.isEmpty()) {
            pendingResult.finish()
            return
        }

        scope.launch {
            try {
                val ds = SleepServiceLocator.getSleepDataSource()
                val sessions = validEvents.map { event ->
                    SleepSession(
                        id = "${event.startTimeMillis}-${event.endTimeMillis}",
                        startTime = event.startTimeMillis,
                        endTime = event.endTimeMillis,
                        source = SleepSource.PHONE_SENSORS,
                        confidence = 1.0f,
                        phase = mapStatusToPhase(event.status),
                    )
                }

                Log.d(TAG, "Saving ${sessions.size} sessions")
                sessions.forEach { session ->
                    Log.d(TAG, "  start=${session.startTime}, end=${session.endTime}, " +
                        "duration=${session.duration}ms, phase=${session.phase.label}")
                    ds.saveSleepSession(session)
                }
                Log.d(TAG, "All sessions saved")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save sleep sessions", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun mapStatusToPhase(status: Int): SleepPhase = when (status) {
        STATUS_AWAKE -> SleepPhase.AWAKE
        STATUS_ASLEEP -> SleepPhase.ASLEEP
        STATUS_LIGHT -> SleepPhase.LIGHT
        STATUS_DEEP -> SleepPhase.DEEP
        STATUS_REM -> SleepPhase.REM
        else -> SleepPhase.UNKNOWN
    }
}
