package com.hazlosano.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.hazlosano.domain.settings.LanguagePreference

/**
 * Hace que todo lo que haya dentro lea sus cadenas en el idioma elegido.
 *
 * Son dos gestos y hacen falta los dos:
 *
 * 1. **Mover el locale de la plataforma** ([applyPlatformLanguage]), que es de donde Compose
 *    Resources saca el idioma. Se hace en la composición y no en un efecto porque tiene que estar
 *    hecho **antes** de que el contenido de dentro resuelva su primera cadena; un `LaunchedEffect`
 *    corre después y dejaría el primer pintado en el idioma anterior.
 * 2. **Remontar el subárbol** con [key]. El entorno de recursos se memoiza con el locale como clave,
 *    pero `Locale.current` no es estado observable: nada dispara la recomposición al cambiarlo. El
 *    `key` fuerza el remonte, y al reconstruirse el entorno ya lee el locale nuevo.
 *
 * Dónde funciona y dónde no está en la nota de [applyPlatformLanguage]: en iOS y en la web el idioma
 * se guarda pero el app sigue al del sistema.
 */
@Composable
fun HazloLanguage(preference: LanguagePreference, content: @Composable () -> Unit) {
    remember(preference) { applyPlatformLanguage(preference.languageTag) }

    key(preference) { content() }
}
