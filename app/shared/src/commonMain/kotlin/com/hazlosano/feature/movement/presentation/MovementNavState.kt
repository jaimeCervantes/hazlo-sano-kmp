package com.hazlosano.feature.movement.presentation

import androidx.compose.runtime.mutableStateListOf

/**
 * Where the movement pillar currently is. A sealed hierarchy (instead of an enum) so a destination
 * can carry what it needs — the session detail needs the session it shows.
 */
sealed interface MovementDestination {
    data object Closed : MovementDestination
    data object Tracker : MovementDestination
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

    val isOpen: Boolean
        get() = backStack.isNotEmpty()

    fun openTracker() {
        push(MovementDestination.Tracker)
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
        val existing = backStack.indexOf(destination)
        if (existing >= 0) {
            while (backStack.size > existing + 1) backStack.removeLast()
            return
        }
        backStack.add(destination)
    }
}
