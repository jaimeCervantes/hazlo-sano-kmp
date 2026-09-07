package com.hazlosano.feature.movement.tracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.feature.movement.model.RecordingState
import com.hazlosano.domain.feature.movement.model.RouteDeviation
import com.hazlosano.domain.feature.movement.model.RouteStanding
import com.hazlosano.domain.feature.movement.model.SessionStats
import com.hazlosano.domain.feature.movement.model.isMoving
import com.hazlosano.domain.feature.movement.model.UserLocation
import com.hazlosano.domain.feature.movement.model.Route
import com.hazlosano.domain.feature.movement.repository.LocationRepository
import com.hazlosano.domain.feature.movement.repository.RecordingController
import com.hazlosano.domain.feature.movement.repository.RouteRepository
import com.hazlosano.domain.feature.movement.usecase.CalculateStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the tracker screen: streams the user's live location for the map and reflects the session
 * being recorded.
 *
 * The recording itself belongs to [RecordingController] — on Android a foreground service — so
 * leaving this screen, backgrounding the app or turning the screen off cannot end a session. This
 * ViewModel only observes it and forwards start/stop.
 */
class TrackerViewModel(
    private val locationRepository: LocationRepository,
    private val recordingController: RecordingController,
    private val routes: RouteRepository,
) : ViewModel() {

    private val stats = CalculateStatsUseCase()

    private val _followedRoute = MutableStateFlow<FollowedRoute?>(null)

    /**
     * La ruta con la que se va a salir, ya con sus puntos cargados, o `null` si se sale sin ninguna.
     *
     * Cargarla es lo único que hace: se dibuja debajo del recorrido real y se recuerda en la sesión.
     * Avisar de un desvío es otra cosa — C3 del backlog — y aquí no ocurre.
     */
    val followedRoute: StateFlow<FollowedRoute?> = _followedRoute.asStateFlow()

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation.asStateFlow()

    val recording: StateFlow<RecordingState> = recordingController.state
    val lastSavedSession: StateFlow<RecordingState?> = recordingController.lastSavedSession

    /**
     * Las cifras de la salida en curso.
     *
     * **Las calcula el mismo caso de uso que las de una salida terminada**, sobre los mismos puntos.
     * Escribir aquí un segundo cálculo «para el directo» habría sido la manera de que el tracker y
     * el detalle acabaran diciendo cosas distintas de la misma salida, y de que las cuatro reglas
     * que `CalculateStatsUseCase` documenta —umbral por velocidad y no por metros, altitud con
     * histéresis, nada medido es nulo y nunca cero, y la altitud rancia calla el desnivel entero—
     * valieran sólo al terminar.
     *
     * Se recalcula entero en cada lectura en vez de acumularse: son dos pasadas sobre la lista, y
     * una salida de tres horas a una lectura cada dos segundos son unos cinco mil puntos. Acumular
     * ahorraría eso a cambio de tener un segundo estado que mantener en pie, y de que las cifras del
     * directo dejaran de ser recalculables desde lo guardado.
     */
    val liveStats: StateFlow<SessionStats> = recordingController.state
        .map { stats(it.traveledPoints, it.elapsedSeconds) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SessionStats())

    private val _captureTrace = MutableStateFlow(false)

    /**
     * Whether the next recording keeps a diagnostic trace. Held here rather than persisted: it is a
     * choice about the outing you are about to record, so starting the app afresh forgetting it is
     * the right behaviour, not a limitation.
     */
    val captureTrace: StateFlow<Boolean> = _captureTrace.asStateFlow()

    private val _routeStanding = MutableStateFlow<RouteStanding>(RouteStanding.Unknown)

    /**
     * Si vas por la ruta o te has salido de ella.
     *
     * Se juzga contra la posicion **cruda** -la misma del punto azul- y no contra la que el filtro
     * confirma: el filtro retiene hasta un minuto antes de soltar un tramo, y un aviso de desvio con
     * un minuto de retraso llega cuando la bifurcacion ya quedo atras. El desvio no acumula nada, asi
     * que no necesita la proteccion que el filtro le da a la distancia.
     */
    val routeStanding: StateFlow<RouteStanding> = _routeStanding.asStateFlow()

    /** Se rehace con cada ruta: la persistencia de un desvio no se hereda de la ruta anterior. */
    private var deviation: RouteDeviation? = null

    private var tracking = false

    fun startTracking() {
        if (tracking) return
        tracking = true
        viewModelScope.launch {
            locationRepository.getLocationUpdates().collect { location ->
                _userLocation.value = location
                _routeStanding.value = deviation
                    ?.standing(location, moving = recording.value.isMoving)
                    ?: RouteStanding.Unknown
            }
        }
    }

    fun setCaptureTrace(enabled: Boolean) {
        _captureTrace.value = enabled
    }

    /**
     * Carga la ruta elegida con sus puntos.
     *
     * Los puntos se cargan **al elegirla y no al arrancar**: si se cargaran al arrancar, el mapa
     * estaría un instante grabando sin enseñar la ruta, y quien pulsa Iniciar no sabría si se cargó.
     */
    fun followRoute(routeId: Long) {
        viewModelScope.launch {
            val route = routes.getRouteWithPoints(routeId).first() ?: return@launch
            deviation = RouteDeviation(route.points)
            _routeStanding.value = RouteStanding.Unknown
            _followedRoute.value = FollowedRoute(
                id = route.id,
                name = route.name,
                points = route.points.map {
                    UserLocation(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        altitude = it.altitude,
                        // Un punto de ruta puede no traer hora —un GPX de un trazado planificado no
                        // la tiene— y aquí no importa: esto sólo se dibuja.
                        timestamp = it.timestamp ?: 0L,
                    )
                },
            )
        }
    }

    /** Salir sin ruta, después de haber elegido una. */
    fun stopFollowingRoute() {
        _followedRoute.value = null
        deviation = null
        _routeStanding.value = RouteStanding.Unknown
    }

    fun startRecording() {
        recordingController.startRecording(
            captureTrace = _captureTrace.value,
            routeId = _followedRoute.value?.id,
        )
    }

    fun stopRecording() {
        recordingController.stopRecording()
    }

    fun acknowledgeSavedSession() {
        recordingController.acknowledgeSavedSession()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** La ruta que se va a seguir, con lo que la pantalla necesita: cómo se llama y por dónde va. */
data class FollowedRoute(
    val id: Long,
    val name: String,
    val points: List<UserLocation>,
)
