package com.hazlosano.core.ui

import java.util.Locale

/**
 * El locale con el que arrancó el proceso, capturado antes de que nadie lo mueva, para que «seguir
 * al sistema» tenga a dónde volver.
 */
private val systemLocale: Locale = Locale.getDefault()

/**
 * Android sigue a `Locale.getDefault()`: `LocaleList.getDefault()` —de donde Compose saca
 * `Locale.current`— se actualiza cuando el locale por defecto de la JVM cambia.
 */
actual fun applyPlatformLanguage(languageTag: String?) {
    Locale.setDefault(languageTag?.let(Locale::forLanguageTag) ?: systemLocale)
}

actual val platformAppliesLanguage: Boolean = true
