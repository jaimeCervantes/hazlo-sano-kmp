package com.hazlosano.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * La paleta activa, para lo que Material 3 no modela: las rampas de los cuatro pilares y los papeles
 * de marca. Lo que sí modela Material —fondo, superficie, primario, error— se lee de
 * `MaterialTheme.colorScheme`, que se deriva de esta misma paleta unas líneas más abajo. Una sola
 * fuente, dos ventanas.
 */
val LocalHazloPalette: ProvidableCompositionLocal<HazloPalette> =
    staticCompositionLocalOf { LightHazloPalette }

/**
 * Traduce la paleta a los papeles de Material 3.
 *
 * Es una traducción, no una copia: `surface` es la elevación 1 del sitio y `background` es su papel,
 * que son cosas distintas —antes los dos valían `#FFFBFF` y por eso una tarjeta no se despegaba del
 * fondo—. `onPrimary` sale de `buttonPrimaryText` y no de un blanco fijo, porque en oscuro el
 * relleno se aclara y el texto tiene que oscurecerse con él.
 */
private fun HazloPalette.toColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = brandGreen,
        onPrimary = buttonPrimaryText,
        primaryContainer = brandGreenSoft,
        onPrimaryContainer = brandGreenInk,
        secondary = brandOrange,
        onSecondary = buttonPrimaryText,
        secondaryContainer = brandOrangeSoft,
        onSecondaryContainer = feedbackErrorInk,
        tertiary = brandGray,
        onTertiary = textBase,
        tertiaryContainer = surfaceElevation2,
        onTertiaryContainer = textBase,
        background = surfaceBackground,
        onBackground = textBase,
        surface = surfaceElevation1,
        onSurface = textBase,
        surfaceVariant = surfaceElevation2,
        onSurfaceVariant = textSupport,
        outline = borderField,
        outlineVariant = border,
        error = feedbackError,
        onError = buttonPrimaryText,
        errorContainer = feedbackErrorSoft,
        onErrorContainer = feedbackErrorInk,
    )
} else {
    lightColorScheme(
        primary = brandGreen,
        onPrimary = buttonPrimaryText,
        primaryContainer = brandGreenSoft,
        onPrimaryContainer = brandGreenInk,
        secondary = brandOrange,
        onSecondary = brandWhite,
        secondaryContainer = brandOrangeSoft,
        onSecondaryContainer = feedbackErrorInk,
        tertiary = brandGray,
        onTertiary = brandWhite,
        tertiaryContainer = surfaceElevation2,
        onTertiaryContainer = textBase,
        background = surfaceBackground,
        onBackground = textBase,
        surface = surfaceElevation1,
        onSurface = textBase,
        surfaceVariant = surfaceElevation2,
        onSurfaceVariant = textSupport,
        outline = borderField,
        outlineVariant = border,
        error = feedbackError,
        onError = brandWhite,
        errorContainer = feedbackErrorSoft,
        onErrorContainer = feedbackErrorInk,
    )
}

@Composable
fun HazloSanoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkHazloPalette else LightHazloPalette
    CompositionLocalProvider(LocalHazloPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(darkTheme),
            typography = hazloTypography(),
            content = content,
        )
    }
}
