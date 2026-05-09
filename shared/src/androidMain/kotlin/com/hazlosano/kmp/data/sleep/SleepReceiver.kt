package com.hazlosano.kmp.data.sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.SleepSegmentEvent
import com.hazlosano.kmp.domain.model.SleepSession
import com.hazlosano.kmp.domain.model.SleepSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SleepReceiver : BroadcastReceiver() {

    companion object {
        var dataSource: SleepDataSource? = null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (!SleepSegmentEvent.hasEvents(intent)) return

        if (!SleepContextProvider.isInitialized()) {
            SleepContextProvider.initialize(context)
        }

        val ds = dataSource ?: createSleepDataSource().also { dataSource = it }

        val events: List<SleepSegmentEvent> = SleepSegmentEvent.extractEvents(intent)
        val sessions = events.mapIndexed { index, event ->
            SleepSession(
                id = "${event.startTimeMillis}-$index-${System.currentTimeMillis()}",
                startTime = event.startTimeMillis,
                endTime = event.endTimeMillis,
                source = SleepSource.PHONE_SENSORS,
                confidence = 1.0f,
            )
        }

        if (sessions.isNotEmpty()) {
            scope.launch {
                sessions.forEach { session -> ds.saveSleepSession(session) }
            }
        }
    }
}
