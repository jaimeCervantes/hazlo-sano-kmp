package com.hazlosano.core.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
 * **Tres cosas hacen que esto se sienta fluido y no animado**, y las tres se aprendieron comparando
 * con el proyecto de referencia, que usa `NavHost` de Navigation Compose:
 *
 * 1. **No se anima el tamaño.** `AnimatedContent` interpola por defecto entre el tamaño del
 *    contenido que sale y el del que entra, y con pantallas completas eso es un contenedor
 *    cambiando de medida mientras su contenido se re-mide en cada fotograma. Es el brinco que se
 *    veía. Se apaga con `using null`.
 * 2. **El desplazamiento es del contenedor, no del contenido.** `slideIntoContainer` mide desde el
 *    hueco donde va la pantalla; `slideInHorizontally` medía desde lo que ocupa el contenido, que
 *    con listas perezosas todavía no ha terminado de medirse cuando arranca la animación.
 * 3. **Los fundidos no duran lo mismo que el deslizamiento ni empiezan a la vez.** Lo que sale se
 *    apaga rápido y lo que entra enciende después: solapar los dos al mismo ritmo deja un tramo con
 *    las dos pantallas medio visibles, que es lo que se lee como turbio.
 */
object HazloTransitions {

    /**
     * Entrar más adentro, o volver de allí.
     *
     * La nueva entra por el lado al que apunta el viaje y la vieja se retira **un cuarto** de
     * pantalla, que es lo que da la sensación de que una queda detrás de la otra en vez de al lado.
     * Yendo hacia atrás las dos invierten el sentido: el movimiento sigue al gesto.
     */
    fun <S> AnimatedContentTransitionScope<S>.depth(goingDeeper: Boolean): ContentTransform {
        val towards = if (goingDeeper) {
            AnimatedContentTransitionScope.SlideDirection.Start
        } else {
            AnimatedContentTransitionScope.SlideDirection.End
        }
        return (
            slideIntoContainer(towards, slideSpec()) + fadeIn(enteringFade())
            ) togetherWith (
            slideOutOfContainer(towards, slideSpec()) { full -> full / RECEDING_FRACTION } +
                fadeOut(leavingFade())
            // El tamaño no se anima: son dos pantallas completas y miden lo mismo. Interpolarlo era
            // el brinco que se veía en el contenido.
            ) using null
    }

    /**
     * Cambiar entre cosas del mismo nivel — las pestañas de pilar.
     *
     * Se funden porque no hay dentro ni fuera: deslizar implicaría un orden espacial entre pilares
     * que no existe, y el usuario aprendería una dirección que no significa nada.
     *
     * Lo que entra crece un poco al aparecer, que es lo que separa un fundido de un parpadeo: sin
     * ese gesto las dos pantallas se cruzan sin que nada indique cuál llega.
     */
    fun <S> AnimatedContentTransitionScope<S>.siblings(): ContentTransform = (
        fadeIn(enteringFade()) + scaleIn(enteringFade(), initialScale = ARRIVING_SCALE)
        // Tambien aqui se apaga el tamaño: dos pilares pueden medir distinto, y sin esto el
        // contenedor interpolaria entre las dos alturas mientras se funden.
        ) togetherWith fadeOut(leavingFade()) using null

    /** Lo que se va se apaga rápido, para dejar sitio antes de que llegue lo que viene. */
    private fun <T> leavingFade(): FiniteAnimationSpec<T> =
        tween(HazloMotion.DURATION_FAST_MILLIS, easing = HazloMotion.standard)

    /** Y lo que llega enciende después, sin solaparse con lo que todavía se está yendo. */
    private fun <T> enteringFade(): FiniteAnimationSpec<T> = tween(
        durationMillis = HazloMotion.DURATION_BASE_MILLIS - HazloMotion.DURATION_FAST_MILLIS,
        delayMillis = HazloMotion.DURATION_FAST_MILLIS,
        easing = HazloMotion.standard,
    )

    private fun <T> slideSpec(): FiniteAnimationSpec<T> =
        tween(HazloMotion.DURATION_BASE_MILLIS, easing = HazloMotion.standard)

    /** Cuánto se retira la pantalla que se va: una cuarta parte, no entera. */
    private const val RECEDING_FRACTION = 4

    /** Casi uno: lo justo para que se note que llega, sin que parezca que salta. */
    private const val ARRIVING_SCALE = 0.94f
}
