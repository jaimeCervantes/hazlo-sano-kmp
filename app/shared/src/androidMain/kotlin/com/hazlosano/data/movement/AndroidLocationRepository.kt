package com.hazlosano.data.movement

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val UPDATE_INTERVAL_MS = 2000L

/**
 * Emits GPS updates from the fused location provider. The caller must hold a location permission
 * before collecting; otherwise the flow closes with the thrown [SecurityException].
 */
class AndroidLocationRepository(private val context: Context) : LocationRepository {

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<UserLocation> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                trySend(
                    UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        // getAltitude() returns 0.0 exactly when hasAltitude() is false, so reading
                        // it without asking turns a fix with no vertical component into a confident
                        // claim of sea level.
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        accuracy = location.accuracy,
                        // What the receiver says about its own altitude, rather than the estimate of
                        // twice the horizontal accuracy the filter falls back to.
                        verticalAccuracy = if (location.hasVerticalAccuracy()) {
                            location.verticalAccuracyMeters
                        } else {
                            null
                        },
                        bearing = location.bearing,
                        timestamp = location.time,
                    ),
                )
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose { client.removeLocationUpdates(callback) }
    }
}
