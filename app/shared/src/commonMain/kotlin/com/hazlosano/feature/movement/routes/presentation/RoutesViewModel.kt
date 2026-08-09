package com.hazlosano.feature.movement.routes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import com.hazlosano.domain.feature.movement.usecase.ExportRouteAsGpxUseCase
import com.hazlosano.domain.feature.movement.usecase.ImportRouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the routes screen is showing and what it has to say about the last thing that happened.
 *
 * [pendingImport] is the duplicate question: the same track arrives twice more often than it
 * sounds, and the screen has to ask before replacing a route the person may still want.
 */
data class RoutesUiState(
    val message: String? = null,
    val pendingImport: PendingImport? = null,
    val exported: ExportedGpx? = null,
)

/** A track that is already stored, waiting on the answer to whether it should be replaced. */
data class PendingImport(
    val existingName: String,
    val incomingName: String,
    val data: ByteArray,
) {
    // ByteArray compares by identity, which would make two states holding the same file look
    // different and re-trigger the dialog on every recomposition.
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is PendingImport &&
                    existingName == other.existingName &&
                    incomingName == other.incomingName &&
                    data.contentEquals(other.data)
                )

    override fun hashCode(): Int =
        (existingName.hashCode() * 31 + incomingName.hashCode()) * 31 + data.contentHashCode()
}

/** A route written out and waiting for the platform to put it somewhere. */
data class ExportedGpx(val fileName: String, val gpx: String)

class RoutesViewModel(
    private val routes: RouteRepository,
    private val importRoute: ImportRouteUseCase,
    private val exportRoute: ExportRouteAsGpxUseCase,
) : ViewModel() {

    val allRoutes: StateFlow<List<Route>> = routes.getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    /**
     * @param fileName the name of the file picked. Used only when the track inside does not name
     * itself, so a route never ends up called "Ruta sin nombre" when the file said more than that.
     */
    fun import(fileName: String, data: ByteArray) {
        val fallback = fileName.substringBeforeLast('.')
        viewModelScope.launch {
            when (val result = importRoute(data, fallbackName = fallback)) {
                is ImportRouteUseCase.Result.Success -> show("Ruta importada: ${result.route.name}")
                is ImportRouteUseCase.Result.Error -> show(result.message)
                is ImportRouteUseCase.Result.AlreadyExists -> _uiState.value = _uiState.value.copy(
                    pendingImport = PendingImport(
                        existingName = result.existingRoute.name,
                        incomingName = result.newRoute.name,
                        data = data,
                    ),
                )
            }
        }
    }

    fun confirmReplace() {
        val pending = _uiState.value.pendingImport ?: return
        _uiState.value = _uiState.value.copy(pendingImport = null)
        viewModelScope.launch {
            val result = importRoute(
                pending.data,
                forceOverwrite = true,
                fallbackName = pending.incomingName,
            )
            when (result) {
                is ImportRouteUseCase.Result.Success -> show("Ruta reemplazada: ${result.route.name}")
                is ImportRouteUseCase.Result.Error -> show(result.message)
                // Overwriting cannot report the route as already existing: that is the point of it.
                is ImportRouteUseCase.Result.AlreadyExists -> show("No se pudo reemplazar la ruta.")
            }
        }
    }

    fun dismissPendingImport() {
        _uiState.value = _uiState.value.copy(pendingImport = null)
    }

    fun rename(routeId: Long, name: String) {
        val chosen = name.trim()
        if (chosen.isEmpty()) {
            show("La ruta necesita un nombre.")
            return
        }
        viewModelScope.launch {
            routes.renameRoute(routeId, chosen)
            show("Ruta renombrada.")
        }
    }

    fun delete(routeId: Long) {
        viewModelScope.launch {
            routes.deleteRoute(routeId)
            show("Ruta eliminada.")
        }
    }

    fun export(routeId: Long) {
        viewModelScope.launch {
            when (val result = exportRoute(routeId)) {
                is ExportRouteAsGpxUseCase.Result.Success -> _uiState.value = _uiState.value.copy(
                    exported = ExportedGpx(result.fileName, result.gpx),
                )
                is ExportRouteAsGpxUseCase.Result.Error -> show(result.message)
            }
        }
    }

    /** Called once the platform has taken the file, so the same export is not handed over twice. */
    fun consumeExport() {
        _uiState.value = _uiState.value.copy(exported = null)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun show(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }
}
