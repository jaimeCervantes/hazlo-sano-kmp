package com.hazlosano.core.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

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

/**
 * Cómo pasa el app de una pantalla a otra.
 *
 * **Una sola regla, y de ella salen las dos formas:** lo que es hermano se funde, lo que es más
 * profundo entra por el lado. No hay una tercera, y ninguna pantalla elige la suya — la elige de
 * dónde a dónde se va.
 *
 * Hasta ahora no había ninguna: las pantallas se sustituían de golpe, y [HazloMotion] llevaba desde
 * el sistema de diseño con las curvas del sitio hermano dentro sin que nadie las usara.
 */
object HazloTransitions {

    /**
     * Entrar más adentro, o volver de allí.
     *
     * La nueva entra entera por el lado al que apunta el viaje y la vieja se retira un cuarto de
     * pantalla, que es lo que da la sensación de que una queda **detrás** de la otra en vez de al
     * lado. Yendo hacia atrás, las dos invierten el sentido: el movimiento sigue al gesto.
     *
     * Curva `standard` y no `natural`: `natural` se hizo para desplazamientos de unos pocos dp, y
     * aquí se mueve la pantalla entera.
     */
    fun depth(goingDeeper: Boolean): ContentTransform {
        val entering = if (goingDeeper) 1 else -1
        return (
            slideInHorizontally(spec()) { width -> entering * width } +
                fadeIn(fadeSpec())
            ) togetherWith (
            slideOutHorizontally(spec()) { width -> -entering * width / RECEDING_FRACTION } +
                fadeOut(fadeSpec())
            )
    }

    /**
     * Cambiar entre cosas del mismo nivel — las pestañas de pilar.
     *
     * Se funden porque no hay dentro ni fuera: deslizar implicaría un orden espacial entre pilares
     * que no existe, y el usuario aprendería una dirección que no significa nada.
     */
    fun siblings(): ContentTransform = fadeIn(fadeSpec()) togetherWith fadeOut(fadeSpec())

    private fun <T> spec(): FiniteAnimationSpec<T> =
        tween(HazloMotion.DURATION_BASE_MILLIS, easing = HazloMotion.standard)

    private fun <T> fadeSpec(): FiniteAnimationSpec<T> =
        tween(HazloMotion.DURATION_FAST_MILLIS, easing = HazloMotion.standard)

    /** Cuánto se retira la pantalla que se va: una cuarta parte, no entera. */
    private const val RECEDING_FRACTION = 4
}
