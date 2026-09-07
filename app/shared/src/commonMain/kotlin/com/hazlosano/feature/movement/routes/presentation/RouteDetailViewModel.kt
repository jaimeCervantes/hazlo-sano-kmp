package com.hazlosano.feature.movement.routes.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.data.movement.routeRepository
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import com.hazlosano.domain.feature.movement.usecase.ExportRouteAsGpxUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Una ruta guardada, con sus puntos, y lo que se puede hacer con ella sin salir de mirarla.
 *
 * Observa el repositorio en vez de leer una vez: si la ruta se borra o se renombra desde otro sitio
 * mientras esta pantalla la enseña, lo que se ve deja de ser una foto vieja.
 *
 * **Descargar y borrar viven también aquí, y no sólo en la lista.** Antes había que volver atrás
 * para hacer cualquier cosa con la ruta que se estaba mirando, que es el sitio donde se decide.
 */
class RouteDetailViewModel(
    private val routeId: Long,
    private val routes: RouteRepository,
    private val exportRoute: ExportRouteAsGpxUseCase,
) : ViewModel() {

    val state: StateFlow<RouteDetailUiState> = routes.getRouteWithPoints(routeId)
        .map(::routeDetail)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = RouteDetailUiState.Loading,
        )

    private val _exported = MutableStateFlow<ExportedGpx?>(null)

    /** Un GPX escrito esperando a que la plataforma decida dónde ponerlo. */
    val exported: StateFlow<ExportedGpx?> = _exported.asStateFlow()

    private val _deleted = MutableStateFlow(false)

    /**
     * La ruta que esta pantalla enseñaba ya no existe.
     *
     * Se dice explícitamente en vez de deducirlo de que el detalle se quede vacío: «no la
     * encuentro» y «acabas de borrarla» son dos situaciones distintas y merecen dos respuestas
     * distintas.
     */
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun export() {
        viewModelScope.launch {
            when (val result = exportRoute(routeId)) {
                is ExportRouteAsGpxUseCase.Result.Success ->
                    _exported.value = ExportedGpx(result.fileName, result.gpx)
                // Aquí no hay dónde contarlo sin tapar el mapa, y el caso es el de una ruta que ya
                // no está — que es justo lo que el detalle va a enseñar por su cuenta.
                is ExportRouteAsGpxUseCase.Result.Failed -> Unit
            }
        }
    }

    /** Called once the platform has taken the file, so the same export is not handed over twice. */
    fun consumeExport() {
        _exported.value = null
    }

    fun delete() {
        viewModelScope.launch {
            routes.deleteRoute(routeId)
            _deleted.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Lo arma con la persistencia que tenga este target, igual que el resto del pilar. La clave del
 * `remember` es el id: abrir otra ruta no puede reutilizar el ViewModel de la anterior.
 */
@Composable
fun rememberRouteDetailViewModel(routeId: Long): RouteDetailViewModel = remember(routeId) {
    val repository = routeRepository()
    RouteDetailViewModel(
        routeId = routeId,
        routes = repository,
        exportRoute = ExportRouteAsGpxUseCase(repository),
    )
}
