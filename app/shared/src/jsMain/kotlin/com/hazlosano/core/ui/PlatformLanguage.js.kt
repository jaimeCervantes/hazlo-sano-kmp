package com.hazlosano.core.ui

/**
 * En la web no se hace nada, y es deliberado.
 *
 * `Locale.current` sale de `navigator.language`, que una página no puede cambiar. El idioma se
 * guarda igualmente —en memoria, como el resto de ajustes en este target— y el app sigue el del
 * navegador.
 */
actual fun applyPlatformLanguage(languageTag: String?) {
    // Sin efecto a propósito. Ver la nota de arriba y la del `expect`.
}

actual val platformAppliesLanguage: Boolean = false
