package com.hazlosano.feature.movement.presentation

/**
 * Los tres sitios del pilar. Un detalle no está aquí: no es un sitio, es algo que abriste.
 *
 * Vivía dentro de `MovementBottomBar`, que es sólo uno de los dos que necesitan la distinción. El
 * otro es la navegación: los tres sitios comparten cromo y se funden entre ellos, y un detalle trae
 * el suyo y entra por el lado. Con el enum encerrado en la barra, la segunda respuesta habría sido
 * una copia de la primera — y dos copias de «qué es un sitio» se separan en cuanto alguien añada un
 * cuarto destino.
 */
enum class MovementPlace {
    Record,
    Routes,
    Outings,
}

/**
 * En cuál de los tres sitios está el pilar, o `null` si está en un detalle.
 *
 * Es **la única definición** de qué cuenta como sitio, y de ella salen las tres cosas que dependen
 * de eso: qué pestaña se marca abajo, si se pinta la barra de abajo, y si el viaje se funde o entra.
 */
fun movementPlaceOf(destination: MovementDestination): MovementPlace? = when (destination) {
    is MovementDestination.Tracker -> MovementPlace.Record
    MovementDestination.Routes -> MovementPlace.Routes
    MovementDestination.History -> MovementPlace.Outings

    is MovementDestination.RouteDetail,
    is MovementDestination.SessionDetail,
    MovementDestination.Closed,
    -> null
}
