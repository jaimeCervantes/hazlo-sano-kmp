package com.hazlosano.feature.movement.routes.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hazlosano.data.movement.routeRepository
import com.hazlosano.domain.feature.movement.parser.GpxFormat
import com.hazlosano.domain.feature.movement.usecase.ExportRouteAsGpxUseCase
import com.hazlosano.domain.feature.movement.usecase.ImportRouteUseCase

/**
 * Wires the routes screen to whichever persistence this target has, matching how the tracker and
 * the history are built. There is no DI container in this project yet, so the composition happens
 * here rather than being spread through the Composables.
 */
@Composable
fun rememberRoutesViewModel(): RoutesViewModel = remember {
    val repository = routeRepository()
    RoutesViewModel(
        routes = repository,
        importRoute = ImportRouteUseCase(repository, GpxFormat),
        exportRoute = ExportRouteAsGpxUseCase(repository),
    )
}
