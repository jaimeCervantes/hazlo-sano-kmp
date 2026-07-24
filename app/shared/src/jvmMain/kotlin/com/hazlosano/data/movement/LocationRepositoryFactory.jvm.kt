package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual fun createLocationRepository(): LocationRepository = object : LocationRepository {
    override fun getLocationUpdates(): Flow<UserLocation> = emptyFlow()
}
