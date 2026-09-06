package com.hazlosano.domain.feature.movement.model

/**
 * Lo que puede salir mal al importar, exportar o guardar una ruta.
 *
 * Existe porque `core` no puede escribir copia. Los tres casos de uso devolvían un
 * `Result.Error(message: String)` con la frase ya redactada en español —"La ruta ya no existe.",
 * "La ruta necesita un nombre."— y desde un módulo neutral esa frase no la alcanza ningún catálogo
 * de traducciones: el día que el app hable inglés, seguiría saliendo en español.
 *
 * Devolver un caso cerrado en vez de una frase mueve la decisión de **cómo se dice** a la UI, que es
 * la única capa que puede leer recursos, y de paso deja que las pruebas afirmen sobre el caso en vez
 * de sobre la redacción — que era lo que las hacía romperse al corregir una coma.
 */
enum class RouteProblem {
    /** El archivo no se pudo leer como GPX. */
    UNREADABLE_GPX,

    /** La ruta que se pedía ya no está guardada. */
    ROUTE_NOT_FOUND,

    /** La ruta existe pero no tiene ningún punto que escribir. */
    ROUTE_HAS_NO_POINTS,

    /** Se pidió guardar sin nombre. */
    NAME_REQUIRED,

    /** La sesión no recorrió lo bastante como para que su trazado sea una ruta. */
    NOT_ENOUGH_POINTS,
}
