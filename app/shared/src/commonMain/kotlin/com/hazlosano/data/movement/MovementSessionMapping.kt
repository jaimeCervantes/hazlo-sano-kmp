package com.hazlosano.data.movement

import com.hazlosano.data.db.MovementPointEntity
import com.hazlosano.data.db.MovementSessionEntity
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.UserLocation

/**
 * Maps a persisted session row to the domain model. Points are loaded separately, and everything
 * the route shows is derived from them when the session is opened rather than read from here.
 */
internal fun MovementSessionEntity.toDomain(
    previewPoints: List<UserLocation> = emptyList(),
): MovementSession =
    MovementSession(
        id = id,
        routeId = routeId,
        name = name,
        date = date,
        elapsedTime = elapsedTime,
        distanceTraveled = distanceTraveled,
        previewPoints = previewPoints,
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
