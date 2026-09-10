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
    Search,
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
 *
 * La búsqueda se abre escribiendo en la barra de arriba, y esa barra sólo existe en el cuerpo
 * principal. Así que **hoy no puede coincidir con ninguna capa salvo ajustes**, que sí se alcanza
 * desde los resultados por el menú de los tres puntos. Su sitio entre Movimiento y la ficha de un
 * pilar no lo decide una carrera que exista: lo decide que ajustes tiene que ganarle y que ella
 * tiene que ganarle al cuerpo. Queda dicho para que nadie lo lea como una regla más fuerte de lo que
 * es.
 */
fun topLayer(
    settingsOpen: Boolean,
    movementOpen: Boolean,
    searchOpen: Boolean,
    pillarInfoOpen: Boolean,
    sleepHistoryOpen: Boolean,
): AppLayer? = when {
    settingsOpen -> AppLayer.Settings
    movementOpen -> AppLayer.Movement
    searchOpen -> AppLayer.Search
    pillarInfoOpen -> AppLayer.PillarInfo
    sleepHistoryOpen -> AppLayer.SleepHistory
    else -> null
}

/**
 * Cuánto adentro está una capa, contando la pantalla principal como cero.
 *
 * **Es la misma escala que decide qué se pinta y qué deshace el gesto de volver atrás**, leída al
 * revés: la que tapa a todas es la que está más adentro. Que también decida hacia dónde se desliza
 * una transición es lo que hace que el movimiento y la navegación no puedan contradecirse — entrar
 * se ve como entrar porque *es* entrar, y no porque alguien lo haya escrito dos veces.
 */
val AppLayer?.depth: Int
    get() = this?.let { AppLayer.entries.size - it.ordinal } ?: 0
