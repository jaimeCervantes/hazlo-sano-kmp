package com.hazlosano.feature.movement.presentation

/**
 * Qué clase de viaje es pasar de una pantalla del pilar a otra.
 *
 * Existe porque la respuesta **no es el tamaño de la pila**, que era de donde se sacaba. Los tres
 * sitios del pilar —Grabar, Rutas, Salidas— se apilan igual que los detalles, así que ir de Grabar a
 * Rutas hacía crecer la pila y se animaba como entrar más adentro. Son hermanos: tienen su propia
 * barra inferior, exactamente igual que las cinco pestañas de pilar.
 */
enum class MovementTransition {
    /** Grabar, Rutas y Salidas entre sí: se funden, como cambiar de pilar. */
    Siblings,

    /** Abrir el detalle de una ruta o de una salida: entra por el lado. */
    Deeper,

    /** Volver de un detalle: sale por donde entró. */
    Back,
}

/**
 * Si este destino es **un sitio** del pilar o algo que abriste desde uno.
 *
 * Es la misma línea que ya traza `MovementBottomBar`, que se pinta «en las tres pantallas que son un
 * sitio y en ninguna más». Que la transición y la barra inferior contesten lo mismo no es una
 * coincidencia que haya que mantener a mano: las dos preguntan esto.
 */
private val MovementDestination.isPlace: Boolean
    get() = when (this) {
        is MovementDestination.Tracker,
        MovementDestination.History,
        MovementDestination.Routes,
        -> true

        is MovementDestination.SessionDetail,
        is MovementDestination.RouteDetail,
        MovementDestination.Closed,
        -> false
    }

/**
 * Cómo animar el paso de [from] a [to].
 *
 * [goingDeeper] lo sabe la pila —si crece se entra, si mengua se sale— y aquí sólo decide el sentido
 * del deslizamiento **cuando hay deslizamiento**. Entre dos sitios no lo hay, pase lo que pase con
 * la pila: ir de Grabar a Rutas y volver de Rutas a Grabar son el mismo gesto lateral en la barra de
 * abajo, y animar uno hacia dentro y el otro hacia fuera enseñaría una dirección que no existe.
 */
fun movementTransition(
    from: MovementDestination,
    to: MovementDestination,
    goingDeeper: Boolean,
): MovementTransition = when {
    from.isPlace && to.isPlace -> MovementTransition.Siblings
    goingDeeper -> MovementTransition.Deeper
    else -> MovementTransition.Back
}
