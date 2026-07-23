package com.hazlosano.domain.feature.movement.repository

import com.hazlosano.domain.feature.movement.model.UserLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getLocationUpdates(): Flow<UserLocation>
}
