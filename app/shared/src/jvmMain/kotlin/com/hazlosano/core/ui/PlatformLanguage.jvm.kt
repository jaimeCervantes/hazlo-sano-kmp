package com.hazlosano.core.ui

import java.util.Locale

/**
 * El locale con el que arrancó la JVM, capturado antes de que nadie lo mueva.
 *
 * Sin esto, «seguir al sistema» no tendría a dónde volver: una vez llamado `Locale.setDefault`, el
 * locale original ya no se puede consultar.
 */
private val systemLocale: Locale = Locale.getDefault()

actual fun applyPlatformLanguage(languageTag: String?) {
    Locale.setDefault(languageTag?.let(Locale::forLanguageTag) ?: systemLocale)
}

actual val platformAppliesLanguage: Boolean = true
