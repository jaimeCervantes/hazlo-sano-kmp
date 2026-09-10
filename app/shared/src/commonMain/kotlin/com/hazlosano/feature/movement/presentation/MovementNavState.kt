package com.hazlosano.feature.movement.presentation

import androidx.compose.runtime.mutableStateListOf

/**
 * Where the movement pillar currently is. A sealed hierarchy (instead of an enum) so a destination
 * can carry what it needs — the session detail needs the session it shows.
 */
sealed interface MovementDestination {
    data object Closed : MovementDestination

    /**
     * El tracker, opcionalmente ya con una ruta cargada.
     *
     * [followRouteId] lo trae quien abre el tracker desde el detalle de una ruta: la intención de
     * seguirla es parte de a dónde se va, no un estado suelto que la pantalla tenga que recoger de
     * otro sitio y alguien tenga que acordarse de limpiar.
     */
    data class Tracker(val followRouteId: Long? = null) : MovementDestination
    data object History : MovementDestination
    data object Routes : MovementDestination
    data class SessionDetail(val sessionId: Long) : MovementDestination
    data class RouteDetail(val routeId: Long) : MovementDestination
}

/**
 * Por dónde va el pilar de Movimiento, con memoria de por dónde se llegó.
 *
 * Antes era un solo destino y cada pantalla decidía a mano a cuál volvía: el *volver* de «Mis
 * salidas» iba siempre al tracker y el de «Mis rutas» siempre al historial. Mientras la única puerta
 * al pilar fuera el tracker eso coincidía con la verdad; desde que se llega también desde la pestaña
 * del pilar, deja de coincidir — se entra desde el pilar y se sale al tracker, que es un sitio en el
 * que no se había estado.
 *
 * Con una pila, «volver» significa lo mismo desde cualquier camino y ninguna pantalla tiene que
 * saber quién la abrió. Vive fuera de los Composables para poder probarla sin levantar una pantalla.
 */
class MovementNavState {

    private val backStack = mutableStateListOf<MovementDestination>()

    val destination: MovementDestination
        get() = backStack.lastOrNull() ?: MovementDestination.Closed

    /**
     * El ultimo sitio donde se estuvo, que sobrevive a cerrar el pilar.
     *
     * Hace falta porque el pilar tarda en irse de la pantalla: mientras la transicion corre, la
     * pantalla que sale sigue compuesta, y `destination` ya dice `Closed`. Sin esto se veria un
     * panel en blanco deslizandose en lugar de la pantalla de la que se viene.
     *
     * Es informacion de navegacion —donde estabamos— y no un apaño de la animacion: la pregunta
     * «de donde vengo» la tiene que contestar quien lleva la cuenta de por donde se ha ido.
     */
    var lastPlace: MovementDestination = MovementDestination.Tracker()
        private set

    val isOpen: Boolean
        get() = backStack.isNotEmpty()

    /**
     * Cuantas pantallas se llevan abiertas dentro del pilar.
     *
     * Lo pregunta la transicion para saber si se esta yendo mas adentro o volviendo. Es la misma
     * pila que recorre el gesto de atras, asi que el movimiento no puede contradecir a la
     * navegacion: si la pila crece, se entra; si mengua, se sale.
     */
    val depth: Int
        get() = backStack.size

    fun openTracker() {
        push(MovementDestination.Tracker())
    }

    /**
     * Empieza una salida siguiendo una ruta, desde donde se estaba mirando.
     *
     * Es un destino distinto de [openTracker] a propósito: abrir el tracker a secas y abrirlo con la
     * ruta de la que se viene no son el mismo sitio, así que la pila no los confunde ni corta uno
     * creyendo que llega al otro.
     */
    fun openTrackerFollowing(routeId: Long) {
        push(MovementDestination.Tracker(followRouteId = routeId))
    }

    fun openHistory() {
        push(MovementDestination.History)
    }

    fun openRoutes() {
        push(MovementDestination.Routes)
    }

    fun openSessionDetail(sessionId: Long) {
        push(MovementDestination.SessionDetail(sessionId))
    }

    fun openRouteDetail(routeId: Long) {
        push(MovementDestination.RouteDetail(routeId))
    }

    /** Vuelve a donde se estaba. Desde la primera pantalla, sale del pilar. */
    fun back() {
        backStack.removeLastOrNull()
        // Al salir del pilar no se toca: `lastPlace` tiene que seguir siendo la pantalla de la que
        // se viene mientras esta se va de la pantalla.
        backStack.lastOrNull()?.let { lastPlace = it }
    }

    /** Sale del pilar de una vez, sin recorrer la pila hacia atrás. */
    fun close() {
        backStack.clear()
    }

    /**
     * Volver a un sitio donde ya se estuvo no apila una segunda copia: corta la pila hasta él.
     *
     * Sin esto, ir y venir entre «Mis rutas» y el detalle de una ruta dejaría la pila creciendo, y
     * salir del pilar costaría un *volver* por cada visita.
     */
    private fun push(destination: MovementDestination) {
        lastPlace = destination
        val existing = backStack.indexOf(destination)
        if (existing >= 0) {
            while (backStack.size > existing + 1) backStack.removeLast()
            return
        }
        backStack.add(destination)
    }
}
