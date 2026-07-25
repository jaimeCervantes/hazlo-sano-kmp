package com.hazlosano.data.movement

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hazlosano.data.currentEpochMilliseconds
import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.usecase.SaveSessionUseCase
import com.hazlosano.domain.time.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Keeps a movement session recording while the app is in the background or the screen is off.
 *
 * The service is a thin platform adapter: the recording rules live in [SessionRecording], and what it
 * records is published through [MovementRecordingStore] for the tracker screen to observe.
 */
class MovementRecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var recording: SessionRecording

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        recording = SessionRecording(
            scope = scope,
            locationRepository = createLocationRepository(),
            saveSession = SaveSessionUseCase(
                movementSessionRepository(),
                TimeProvider { currentEpochMilliseconds() },
            ),
            timeProvider = TimeProvider { currentEpochMilliseconds() },
        )

        scope.launch {
            recording.state.collect { state ->
                MovementRecordingStore.publish(state)
                if (state.isRecording) updateNotification(state)
            }
        }
        scope.launch {
            recording.lastSavedSession.collect(MovementRecordingStore::publishSavedSession)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            // No action means the system recreated the service on its own. Whatever it was
            // recording is gone — nothing is persisted to resume from — so shut down instead of
            // lingering as a foreground service that records nothing.
            else -> stopIfIdle()
        }
        // Not sticky for the same reason: being restarted without the session cannot restore it.
        // Surviving a process kill needs the in-progress recording to be persisted first.
        return START_NOT_STICKY
    }

    private fun start() {
        startForeground(NOTIFICATION_ID, buildNotification(RecordingState()))
        recording.start()
    }

    private fun stop() {
        val saving = recording.stop()
        // Shut down only once the session is on disk: tearing the service down first would cancel
        // the write that the whole recording was for.
        scope.launch {
            saving?.join()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** Never tears down a live recording: only shuts down a service that has nothing to record. */
    private fun stopIfIdle() {
        if (recording.state.value.isRecording) return
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(state: RecordingState) {
        getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: RecordingState): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Grabando tu movimiento")
            .setContentText(state.summary())
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Grabación de movimiento",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "movement_recording"
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_START = "com.hazlosano.movement.START_RECORDING"
        private const val ACTION_STOP = "com.hazlosano.movement.STOP_RECORDING"

        fun start(context: Context) {
            context.startForegroundService(intent(context, ACTION_START))
        }

        fun stop(context: Context) {
            context.startService(intent(context, ACTION_STOP))
        }

        private fun intent(context: Context, action: String): Intent =
            Intent(context, MovementRecordingService::class.java).setAction(action)
    }
}

private fun RecordingState.summary(): String {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val distance = if (distanceMeters < 1_000) {
        "${distanceMeters.roundToInt()} m"
    } else {
        "${(distanceMeters / 100).roundToInt() / 10.0} km"
    }
    return "$distance · $minutes:${seconds.toString().padStart(2, '0')}"
}
