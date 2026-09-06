package com.hazlosano.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.newsreader
import hazlosano.app.shared.generated.resources.plus_jakarta_sans
import org.jetbrains.compose.resources.Font

/*
 * Las dos voces de la marca, portadas del sitio (`typography.css`).
 *
 * Hasta aquí el app iba con `FontFamily.Default` y **un solo estilo declarado**, así que sonaba a lo
 * mismo que cualquier panel de administración. El sitio ya había decidido otra cosa, y la decisión
 * tiene forma de reparto: `display` es lo que la marca **afirma** —titulares, nombres de pilar,
 * precios— y `ui` es lo que la interfaz **opera** —etiquetas, botones, cuerpo, datos—.
 *
 * Las dos van empaquetadas y no se le piden a ningún servidor de fuentes: este app se usa en el
 * monte, y una fuente que no carga es una pantalla sin texto.
 */

/** Los pesos de la escala del sitio: 400, 500, 600 y 700. */
private val WEIGHTS = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
)

/**
 * Registra un peso de una fuente variable fijando su eje `wght`.
 *
 * Los dos archivos son **variables** porque Google Fonts ya no publica instancias estáticas de estas
 * familias. En Android eso pide API 26 y el `minSdk` del proyecto es 24: en 24 y 25 el eje se ignora
 * y las dos caras salen en su instancia por defecto (400), con el negrita sintetizado por el
 * sistema. Es una degradación visible sólo en Android 7, y el precio de la alternativa era
 * empaquetar siete archivos estáticos que ya no se distribuyen.
 */
@Composable
private fun variableFamily(resource: org.jetbrains.compose.resources.FontResource): FontFamily =
    FontFamily(
        WEIGHTS.map { weight ->
            Font(
                resource = resource,
                weight = weight,
                style = FontStyle.Normal,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        },
    )

/** Newsreader: la serif editorial. Lo que la marca afirma. */
@Composable
fun hazloDisplayFamily(): FontFamily = variableFamily(Res.font.newsreader)

/** Plus Jakarta Sans: la sans humanista. Lo que la interfaz opera. */
@Composable
fun hazloUiFamily(): FontFamily = variableFamily(Res.font.plus_jakarta_sans)

/*
 * La escala del sitio, mapeada sobre los estilos de Material 3.
 *
 * Los tamaños son los suyos (`--fs-*`): 10, 12, 14, 16, 18, 20, 26, 40 y 56. Material tiene quince
 * estilos y el sitio nueve tamaños, así que el mapeo agrupa: los tres `display` y `headline` grandes
 * hablan con la serif, y de `title` hacia abajo manda la sans. La frontera no es de tamaño sino de
 * trabajo — un `titleMedium` es el nombre de una sección, y eso lo opera la interfaz.
 */
@Composable
fun hazloTypography(): Typography {
    val display = hazloDisplayFamily()
    val ui = hazloUiFamily()

    fun serif(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Bold) = TextStyle(
        fontFamily = display,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

    fun sans(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = ui,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
    )

    return Typography(
        // --fs-display 56 y --fs-heading-lg 40: portada. La voz de la marca.
        displayLarge = serif(56, 64),
        displayMedium = serif(40, 48),
        displaySmall = serif(32, 40),
        // --fs-heading-md 26 y --fs-heading-sm 20.
        headlineLarge = serif(32, 40),
        headlineMedium = serif(26, 34),
        headlineSmall = serif(20, 28),
        // De aquí abajo opera la interfaz.
        titleLarge = sans(22, 28, FontWeight.SemiBold),
        titleMedium = sans(18, 24, FontWeight.SemiBold),
        titleSmall = sans(16, 22, FontWeight.SemiBold),
        // --fs-body-lg 18, --fs-body-md 16, --fs-label 14. Interlineado 1.5, el `--lh-normal`.
        bodyLarge = sans(18, 27),
        bodyMedium = sans(16, 24),
        bodySmall = sans(14, 21),
        // --fs-label 14, --fs-caption 12, --fs-tiny 10.
        labelLarge = sans(14, 20, FontWeight.Medium),
        labelMedium = sans(12, 16, FontWeight.Medium),
        labelSmall = sans(10, 14, FontWeight.Medium),
    )
}
