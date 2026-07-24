package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.repository.LocationRepository

actual fun createLocationRepository(): LocationRepository =
    AndroidLocationRepository(MovementServiceLocator.requireContext())
