package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.Flow

class GetRoutesUseCase(
    private val routeRepository: RouteRepository
) {
    operator fun invoke(): Flow<List<Route>> = routeRepository.getAllRoutes()
}
