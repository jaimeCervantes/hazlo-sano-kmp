package com.hazlosano.feature.movement.detail.presentation

import com.hazlosano.domain.feature.movement.model.RouteProblem

/**
 * Lo que guardar una salida como ruta tiene que decir.
 *
 * Era el ejemplo que `AGENTS.md` cita por su nombre —un ViewModel devolviendo `"Ruta importada: …"`—
 * y por eso se arregla aquí y no en la pantalla: la frase vivía en la capa de presentación, donde
 * ningún catálogo la alcanza.
 */
sealed interface SaveRouteMessage {
    /** El nombre viaja dentro porque no es copia: lo escribió la persona. */
    data class Saved(val routeName: String) : SaveRouteMessage

    data class Failed(val problem: RouteProblem) : SaveRouteMessage
}
