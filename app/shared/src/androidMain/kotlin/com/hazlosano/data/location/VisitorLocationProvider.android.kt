package com.hazlosano.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.hazlosano.data.movement.MovementServiceLocator
import com.hazlosano.domain.model.VisitorLocation
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual suspend fun readVisitorLocation(): VisitorLocation? {
    val context = runCatching { MovementServiceLocator.requireContext() }.getOrNull() ?: return null
    if (!context.hasAnyLocationPermission()) return null

    return runCatching { context.lastKnownLocation() }.getOrNull()
}

/**
 * Basta con la aproximada.
 *
 * El catálogo ordena por cercanía, y para eso `ACCESS_COARSE_LOCATION` sobra. Exigir la fina
 * dejaría sin distancias a quien concedió solo la aproximada, que es una elección deliberada del
 * usuario y no una carencia que haya que castigar.
 */
private fun Context.hasAnyLocationPermission(): Boolean {
    val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

/**
 * `lastLocation` y no una petición de actualizaciones: no enciende el receptor ni espera un fix.
 *
 * Se resuelve con `null` tanto si falla como si el sistema no tiene ninguna guardada — un teléfono
 * recién encendido puede no tenerla, y eso no es un error que reportar sino un catálogo por fecha.
 */
private suspend fun Context.lastKnownLocation(): VisitorLocation? =
    suspendCancellableCoroutine { continuation ->
        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location ->
                continuation.resume(
                    location?.let {
                        VisitorLocation(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            fixedAtEpochMillis = it.time.takeIf { time -> time > 0L },
                        )
                    },
                )
            }
            .addOnFailureListener { continuation.resume(null) }
    }
