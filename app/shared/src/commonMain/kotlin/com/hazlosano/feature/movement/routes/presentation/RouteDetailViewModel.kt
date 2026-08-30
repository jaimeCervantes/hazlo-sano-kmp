package com.hazlosano.feature.movement.routes.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.data.movement.routeRepository
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Una ruta guardada, con sus puntos.
 *
 * Observa el repositorio en vez de leer una vez: si la ruta se borra o se renombra desde otro sitio
 * mientras esta pantalla la enseña, lo que se ve deja de ser una foto vieja.
 */
class RouteDetailViewModel(
    routeId: Long,
    routes: RouteRepository,
) : ViewModel() {

    val state: StateFlow<RouteDetailUiState> = routes.getRouteWithPoints(routeId)
        .map(::routeDetail)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = RouteDetailUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Lo arma con la persistencia que tenga este target, igual que el resto del pilar. La clave del
 * `remember` es el id: abrir otra ruta no puede reutilizar el ViewModel de la anterior.
 */
@Composable
fun rememberRouteDetailViewModel(routeId: Long): RouteDetailViewModel =
    remember(routeId) { RouteDetailViewModel(routeId, routeRepository()) }
