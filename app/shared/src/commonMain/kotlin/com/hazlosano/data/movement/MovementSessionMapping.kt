package com.hazlosano.data.movement

import com.hazlosano.data.db.MovementPointEntity
import com.hazlosano.data.db.MovementSessionEntity
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation

/**
 * Maps a persisted session row to the domain model. Points are loaded separately, and everything
 * the route shows is derived from them when the session is opened rather than read from here.
 *
 * La silueta sí sale de la fila, y es la segunda excepción declarada junto a `distanceTraveled`: la
 * lista la necesita para dibujarse y no puede leer todos los puntos de todas las salidas. Una salida
 * guardada antes de que existiera la columna vuelve con la silueta vacía, y la lista enseña su hueco.
 */
internal fun MovementSessionEntity.toDomain(): MovementSession =
    MovementSession(
        id = id,
        routeId = routeId,
        name = name,
        date = date,
        elapsedTime = elapsedTime,
        distanceTraveled = distanceTraveled,
        previewPoints = TrackPreviewFormat.decode(previewPoints),
    )

internal fun MovementPointEntity.toDomain(): UserLocation =
    UserLocation(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        accuracy = accuracy.toFloat(),
        verticalAccuracy = verticalAccuracy?.toFloat(),
        bearing = bearing.toFloat(),
        timestamp = timestamp,
    )
