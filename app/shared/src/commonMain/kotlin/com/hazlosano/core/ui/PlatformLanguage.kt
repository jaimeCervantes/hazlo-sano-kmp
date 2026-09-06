package com.hazlosano.core.ui

/**
 * Fija el idioma con el que la plataforma responde a `Locale.current`, o lo devuelve al del sistema
 * cuando [languageTag] es nulo.
 *
 * **Por qué pasa por la plataforma y no por Compose Resources.** El camino directo sería proveer un
 * `ComposeEnvironment` propio con otro `LanguageQualifier`; en Compose Multiplatform 1.11 tanto esa
 * interfaz como `LocalComposeEnvironment` y el constructor de `ResourceEnvironment` son **internos**,
 * así que no se puede construir ni sustituir el entorno desde fuera de la librería.
 *
 * Lo que sí es alcanzable es su entrada: el entorno por defecto se calcula a partir de
 * `androidx.compose.ui.text.intl.Locale.current` y se memoiza **con ese locale como clave**. Mover el
 * locale de la plataforma mueve por tanto el idioma de los recursos, sin tocar nada interno.
 *
 * **Dónde funciona y dónde no**, dicho aquí para que nadie lo descubra en un dispositivo:
 *
 * - **Android y escritorio:** funciona en caliente. `Locale.current` sale del locale por defecto de
 *   la JVM en los dos.
 * - **iOS:** no hace nada. `Locale.current` sale de `NSLocale.currentLocale`, que se cambia
 *   escribiendo `AppleLanguages` en las preferencias y **sólo surte efecto al reiniciar el app**.
 * - **Web:** no hace nada. Sale de `navigator.language`, que la página no puede cambiar.
 *
 * En iOS y web el ajuste sigue guardándose y el app sigue el idioma del sistema, que es lo que hacía
 * antes: se degrada a lo anterior en vez de romperse.
 */
expect fun applyPlatformLanguage(languageTag: String?)

/**
 * Si en esta plataforma elegir idioma cambia algo.
 *
 * Se expone para poder **decirlo en la pantalla** donde no lo cambia, en vez de dejar tres opciones
 * que se marcan y no hacen nada.
 */
expect val platformAppliesLanguage: Boolean
