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
 *
 * [isImporting] cubre la espera entera y no sólo el trabajo propio. Se midió: parsear y guardar un
 * GPX de 20.000 puntos son 378 ms, mientras que el selector de archivos de Android puede tardar
 * varios segundos si el archivo está en la nube y hay que bajarlo. Contar sólo lo segundo dejaría
 * sin explicar justo la parte que se sufre.
 */
data class RoutesUiState(
    val message: RoutesMessage? = null,
    val pendingImport: PendingImport? = null,
    val exported: ExportedGpx? = null,
    val isImporting: Boolean = false,
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
     * Se ha pedido un archivo y todavía no ha llegado.
     *
     * La espera empieza aquí y no cuando llegan los bytes: entre pulsar y recibirlos está el
     * selector del sistema, que es la parte lenta. Se avisa antes de abrirlo para que el hueco no
     * quede sin explicar.
     */
    fun importRequested() {
        _uiState.value = _uiState.value.copy(isImporting = true, message = null)
    }

    /**
     * Ya no va a llegar nada: se cerró el selector sin elegir, o el archivo no se pudo leer.
     *
     * Sin esto, cancelar dejaría la pantalla esperando para siempre por algo que nadie va a mandar.
     */
    fun importAbandoned() {
        _uiState.value = _uiState.value.copy(isImporting = false)
    }

    /**
     * @param fileName the name of the file picked. Used only when the track inside does not name
     * itself, so a route never ends up called "Ruta sin nombre" when the file said more than that.
     */
    fun import(fileName: String, data: ByteArray) {
        val fallback = fileName.substringBeforeLast('.')
        _uiState.value = _uiState.value.copy(isImporting = true)
        viewModelScope.launch {
            when (val result = importRoute(data, fallbackName = fallback)) {
                is ImportRouteUseCase.Result.Success -> show(RoutesMessage.Imported(result.route.name))
                is ImportRouteUseCase.Result.Failed -> show(RoutesMessage.Failed(result.problem))
                // La pregunta del duplicado pasa a ser lo que está ocurriendo: la espera termina
                // aquí, porque ahora se espera a una persona y no a un archivo.
                is ImportRouteUseCase.Result.AlreadyExists -> _uiState.value = _uiState.value.copy(
                    isImporting = false,
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
        _uiState.value = _uiState.value.copy(pendingImport = null, isImporting = true)
        viewModelScope.launch {
            val result = importRoute(
                pending.data,
                forceOverwrite = true,
                fallbackName = pending.incomingName,
            )
            when (result) {
                is ImportRouteUseCase.Result.Success -> show(RoutesMessage.Replaced(result.route.name))
                is ImportRouteUseCase.Result.Failed -> show(RoutesMessage.Failed(result.problem))
                // Overwriting cannot report the route as already existing: that is the point of it.
                is ImportRouteUseCase.Result.AlreadyExists -> show(RoutesMessage.ReplaceFailed)
            }
        }
    }

    fun dismissPendingImport() {
        _uiState.value = _uiState.value.copy(pendingImport = null)
    }

    fun rename(routeId: Long, name: String) {
        val chosen = name.trim()
        if (chosen.isEmpty()) {
            show(RoutesMessage.NameRequired)
            return
        }
        viewModelScope.launch {
            routes.renameRoute(routeId, chosen)
            show(RoutesMessage.Renamed)
        }
    }

    fun delete(routeId: Long) {
        viewModelScope.launch {
            routes.deleteRoute(routeId)
            show(RoutesMessage.Deleted)
        }
    }

    fun export(routeId: Long) {
        viewModelScope.launch {
            when (val result = exportRoute(routeId)) {
                is ExportRouteAsGpxUseCase.Result.Success -> _uiState.value = _uiState.value.copy(
                    exported = ExportedGpx(result.fileName, result.gpx),
                )
                is ExportRouteAsGpxUseCase.Result.Failed -> show(RoutesMessage.Failed(result.problem))
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

    /**
     * Tener algo que decir es haber terminado, así que aquí se apaga la espera.
     *
     * Está en un solo sitio a propósito: si cada desenlace de la importación tuviera que acordarse
     * de apagarla, el que se olvidara dejaría la pantalla esperando para siempre — y el desenlace
     * que más se olvida es el del error.
     */
    private fun show(message: RoutesMessage) {
        _uiState.value = _uiState.value.copy(message = message, isImporting = false)
    }
}
