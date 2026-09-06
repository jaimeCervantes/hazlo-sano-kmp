package com.hazlosano.domain.settings

/**
 * Qué tema quiere ver esta persona.
 *
 * [SYSTEM] no es "ninguno de los dos": es una elección con contenido —seguir al teléfono— y por eso
 * es un valor y no la ausencia de valor. Es también el de partida, porque un app recién instalado
 * no sabe nada de quien lo abre y lo que el teléfono ya decidió es la mejor apuesta disponible.
 */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        val DEFAULT: ThemePreference = SYSTEM

        /**
         * Lee una preferencia guardada, cayendo en [DEFAULT] si no se reconoce.
         *
         * Un valor que no se entiende no puede tumbar el arranque del app, y tampoco puede
         * quedarse a medias: se vuelve a seguir al sistema, que es de donde se partía.
         */
        fun fromStoredValue(value: String?): ThemePreference =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }

    /** Cómo se guarda. Es el nombre del valor, estable a propósito: la base lo lee después. */
    val storedValue: String get() = name
}

/**
 * Si esta preferencia se pinta en oscuro, dado lo que diga el sistema.
 *
 * Vive en `core` y no en el tema porque es la única decisión de esto: el resto es fontanería.
 * Separarla la hace comprobable sin levantar una composición.
 */
fun ThemePreference.resolvesToDark(systemIsDark: Boolean): Boolean = when (this) {
    ThemePreference.SYSTEM -> systemIsDark
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}
