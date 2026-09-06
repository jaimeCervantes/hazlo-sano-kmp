package com.hazlosano.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object HazloSpaces {
    val default: Dp = 0.dp
    val unit: Dp = 8.dp
    val xs: Dp = 4.dp
    val sm: Dp = 12.dp
    val md: Dp = 24.dp
    val lg: Dp = 48.dp
    val xl: Dp = 80.dp
    val gutter: Dp = 24.dp
    val marginMobile: Dp = 20.dp
    val marginDesktop: Dp = 64.dp
}

/**
 * Los radios, nombrados por **qué redondean** y no por cuánto miden.
 *
 * Es la escala del sitio (`layout.css`). Un componente que pide `card` sigue siendo correcto el día
 * que la tarjeta cambie de radio, y ese día se cambia aquí una vez; con la escala anterior —`sm`,
 * `md`, `lg`, `xl`— cada pantalla tenía que decidir un número, y así acabaron conviviendo tarjetas
 * de 16 y de 24 sin que la diferencia significara nada.
 *
 * No se porta el espejo de Tailwind que el sitio mantiene junto a esta escala: allí existe porque
 * sus utilidades (`rounded-lg`) referencian esas variables por nombre, que es un problema de CSS y
 * no nuestro.
 */
object HazloShapes {
    /** Chip, insignia, campo pequeño. */
    val chip: Dp = 8.dp

    /** Botón, campo de texto, tesela de icono. */
    val control: Dp = 12.dp

    /** Tarjeta. */
    val card: Dp = 18.dp

    /** Panel, diálogo, hoja inferior. */
    val panel: Dp = 26.dp

    /** Completamente redondeado. */
    val pill: Dp = 9999.dp
}

/**
 * Las sombras dejan de ser negras.
 *
 * Un `rgba(0,0,0,·)` sobre un papel cálido no se ve neutro: tira a gris y ensucia el fondo. Estas
 * llevan el mismo verde del papel, y se abren más y más suaves según suben — una sombra sugiere
 * altura, no dibuja un borde.
 */
object HazloElevation {
    val xs: Dp = 1.dp
    val sm: Dp = 2.dp
    val md: Dp = 6.dp
    val lg: Dp = 18.dp
    val xl: Dp = 28.dp

    /** El tinte de toda sombra del app: el verde del papel, nunca negro. */
    val tint: Color = Color(0xFF1F2818)
}
