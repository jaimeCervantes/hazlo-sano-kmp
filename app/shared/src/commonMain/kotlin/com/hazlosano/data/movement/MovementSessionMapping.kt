package com.hazlosano.data.movement

import com.hazlosano.data.db.MovementPointEntity
import com.hazlosano.data.db.MovementSessionEntity
import com.hazlosano.domain.feature.movement.model.MovementSession
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.UserLocation

/** Maps a persisted session row to the domain model. Points are loaded separately. */
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
        elevationGain = elevationGain,
        previewPoints = previewPoints,
        stats = SessionStats(
            maxAltitude = maxAltitude,
            minAltitude = minAltitude,
            totalAscent = totalAscent,
            totalDescent = totalDescent,
            avgPace = avgPace,
            movingTime = movingTime,
        ),
    )

internal fun MovementPointEntity.toDomain(): UserLocation =
    UserLocation(
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        accuracy = accuracy.toFloat(),
        bearing = bearing.toFloat(),
        timestamp = timestamp,
    )
