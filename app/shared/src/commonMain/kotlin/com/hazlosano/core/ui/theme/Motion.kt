package com.hazlosano.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Las curvas y las duraciones del sitio (`layout.css`).
 *
 * `natural` arranca rápido y frena largo, que es como se mueve algo que tiene peso: con ella nada
 * se desplaza más de unos pocos dp, así que la pantalla no rebota — crece.
 */
object HazloMotion {
    val standard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val natural: Easing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

    /** Color, borde, opacidad. */
    const val DURATION_FAST_MILLIS: Int = 150

    /** Elevación, entrada de un elemento. */
    const val DURATION_BASE_MILLIS: Int = 260
}
