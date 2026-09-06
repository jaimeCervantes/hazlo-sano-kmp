package com.hazlosano.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * La razón de contraste de WCAG 2.1 entre dos colores opacos.
 *
 * Portada de `contrast.ts` del repo hermano, que es quien mide su propia paleta. Se trae la fórmula
 * y no el resultado: una tabla de cifras copiada a mano deja de ser cierta en cuanto alguien retoca
 * un hex, que es exactamente cómo el app acabó con tres pilares por debajo de AA sin que nadie se
 * enterara.
 */
internal fun contrastRatio(foreground: Color, background: Color): Double {
    val lighter = maxOf(foreground.relativeLuminance(), background.relativeLuminance())
    val darker = minOf(foreground.relativeLuminance(), background.relativeLuminance())
    return (lighter + 0.05) / (darker + 0.05)
}

private fun Color.relativeLuminance(): Double =
    0.2126 * red.toDouble().linearize() +
        0.7152 * green.toDouble().linearize() +
        0.0722 * blue.toDouble().linearize()

private fun Double.linearize(): Double =
    if (this <= 0.03928) this / 12.92 else ((this + 0.055) / 1.055).pow(2.4)

/** El mínimo de WCAG AA para texto normal, y el que este proyecto exige a todo par de color. */
internal const val AA_NORMAL_TEXT: Double = 4.5
