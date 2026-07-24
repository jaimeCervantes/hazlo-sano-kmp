package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.repository.LocationRepository

/**
 * Creates the platform [LocationRepository]. Android provides GPS updates; other targets return a
 * no-op repository until they gain a location integration.
 */
expect fun createLocationRepository(): LocationRepository
