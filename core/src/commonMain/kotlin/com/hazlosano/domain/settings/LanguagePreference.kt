package com.hazlosano.domain.settings

/**
 * En qué idioma quiere leer el app esta persona.
 *
 * Mismo diseño que [ThemePreference], y por el mismo motivo: [SYSTEM] es una elección con contenido
 * —seguir al teléfono— a la que se puede **volver** después de haber fijado uno, y esa vuelta sería
 * inexpresable si «seguir al sistema» fuera la ausencia de valor.
 */
enum class LanguagePreference(
    /**
     * La etiqueta de idioma con la que se buscan los recursos, o `null` cuando se sigue al sistema.
     *
     * Es el código ISO 639-1, que es lo que nombra las carpetas del catálogo (`values`, `values-en`).
     */
    val languageTag: String?,
) {
    SYSTEM(null),
    SPANISH("es"),
    ENGLISH("en"),
    ;

    companion object {
        val DEFAULT: LanguagePreference = SYSTEM

        /** Un valor guardado que no se reconoce vuelve a seguir al sistema en vez de estallar. */
        fun fromStoredValue(value: String?): LanguagePreference =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }

    /** Cómo se guarda. Es el nombre del valor, estable a propósito: la base lo lee después. */
    val storedValue: String get() = name
}
