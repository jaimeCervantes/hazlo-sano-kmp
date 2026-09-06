package com.hazlosano.core.ui

/**
 * En iOS no se hace nada, y es deliberado.
 *
 * `Locale.current` sale de `NSLocale.currentLocale`, que no se puede mover en caliente: se cambia
 * escribiendo `AppleLanguages` en `NSUserDefaults` y sólo surte efecto al reiniciar el app. Un
 * ajuste que exige cerrar y abrir sin decirlo es peor que uno que sigue al sistema, así que aquí el
 * idioma se guarda pero el app sigue leyendo el del teléfono.
 */
actual fun applyPlatformLanguage(languageTag: String?) {
    // Sin efecto a propósito. Ver la nota de arriba y la del `expect`.
}

actual val platformAppliesLanguage: Boolean = false
