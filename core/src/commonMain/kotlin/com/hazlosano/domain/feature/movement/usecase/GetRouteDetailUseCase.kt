package com.hazlosano.domain.feature.movement.usecase

import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.Flow

class GetRouteDetailUseCase(
    private val routeRepository: RouteRepository
) {
    operator fun invoke(routeId: Long): Flow<Route?> = routeRepository.getRouteWithPoints(routeId)
}
