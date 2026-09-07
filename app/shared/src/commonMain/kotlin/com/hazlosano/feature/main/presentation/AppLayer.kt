package com.hazlosano.feature.main.presentation

/**
 * Qué hay encima de la pantalla principal ahora mismo.
 *
 * El app no tiene un grafo de navegación: tiene banderas sueltas en `MainScreen` —ajustes abiertos,
 * el pilar de Movimiento abierto, la ficha de un pilar, el historial de sueño— y la que se pinta es
 * la primera que se comprueba. Esto le pone nombre a ese orden.
 */
enum class AppLayer {
    Settings,
    Movement,
    PillarInfo,
    SleepHistory,
}

/**
 * La capa que se está viendo, o `null` si se está en la pantalla principal.
 *
 * **Existe para que pintar y volver atrás no puedan discrepar.** Son la misma pregunta contestada
 * dos veces —qué hay encima— y hasta ahora sólo se contestaba al pintar, con una cadena de `return`
 * en `MainScreen`; el gesto de volver habría necesitado repetir ese orden a mano, y dos copias del
 * mismo orden se separan en cuanto alguien mueve un bloque. Con una sola función, mover el orden lo
 * mueve para los dos.
 *
 * Ese riesgo no es teórico: en el slice 3 hubo que subir la comprobación de ajustes por encima del
 * `when` de movimiento porque estaba debajo y no llegaba a pintarse nunca.
 *
 * El orden va de lo más reciente a lo más antiguo, que es lo que hace que volver atrás deshaga las
 * cosas en el orden inverso al que se hicieron: ajustes se abre **desde** cualquier sitio, así que
 * tapa a todos; el pilar de Movimiento tapa a la ficha de un pilar por lo mismo.
 */
fun topLayer(
    settingsOpen: Boolean,
    movementOpen: Boolean,
    pillarInfoOpen: Boolean,
    sleepHistoryOpen: Boolean,
): AppLayer? = when {
    settingsOpen -> AppLayer.Settings
    movementOpen -> AppLayer.Movement
    pillarInfoOpen -> AppLayer.PillarInfo
    sleepHistoryOpen -> AppLayer.SleepHistory
    else -> null
}
