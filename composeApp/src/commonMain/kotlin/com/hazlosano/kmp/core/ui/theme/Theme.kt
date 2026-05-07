package com.hazlosano.kmp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HazloSanoGreen,
    onPrimary = Color.White,
    primaryContainer = Green40,
    onPrimaryContainer = Green80,
    secondary = HazloSanoOrange,
    onSecondary = Color.White,
    tertiary = Green40,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = HazloSanoGreen,
    onPrimary = Color.White,
    primaryContainer = GreenGrey80,
    onPrimaryContainer = Green40,
    secondary = HazloSanoOrange,
    onSecondary = Color.White,
    tertiary = GreenGrey40,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
)

@Composable
fun HazloSanoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
