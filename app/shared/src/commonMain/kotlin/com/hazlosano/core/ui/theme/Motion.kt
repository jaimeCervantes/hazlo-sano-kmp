package com.hazlosano.core.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

    /**
     * Pasar de una pantalla a otra. **Es el dial de toda la navegación del app.**
     *
     * Los 260 ms de [DURATION_BASE_MILLIS] se quedaban cortos para una pantalla entera: a esa
     * velocidad el cambio se lee como un corte y no como un movimiento. El proyecto hermano de
     * Android no declara transiciones y se queda con las de Navigation Compose, 700 ms, que es lo
     * que el usuario reconoció como más fluido — pero 700 ms por cada toque de la barra inferior es
     * demasiado para algo que se hace veinte veces seguidas.
     *
     * 450 ms es el punto entre los dos. Si hace falta moverlo, se mueve **aquí y sólo aquí**.
     */
    const val DURATION_SCREEN_MILLIS: Int = 450
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
 *
 * **Lo tercero valía para [depth] y no para [siblings], y se corrigió al compararlo con el proyecto
 * hermano**: cuando lo único que cambia es la opacidad, el relevo deja un instante sin ninguna de
 * las dos y eso parpadea. La salida no era volver a solapar —que es lo turbio— sino que la que llega
 * se encienda **encima** de la que se va, que aguanta opaca debajo. Ver [siblings].
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
     * Cambiar entre cosas del mismo nivel — las pestañas de pilar, los tres sitios de Movimiento.
     *
     * Se funden porque no hay dentro ni fuera: deslizar implicaría un orden espacial entre pilares
     * que no existe, y el usuario aprendería una dirección que no significa nada.
     *
     * **Lo que llega se enciende encima de lo que se va, que se queda opaco debajo hasta el final.**
     * Ni relevo ni fundido cruzado, y las dos alternativas están descartadas por una razón cada una:
     *
     * - **El relevo** —apagar lo viejo y luego encender lo nuevo— es lo que había aquí, y deja un
     *   instante en que ninguna de las dos está entera. Sobre fondo oscuro eso parpadea, y fue lo
     *   que obligó a añadir un `scaleIn` para disimularlo. El gesto sobra cuando no hay parpadeo que
     *   tapar, así que se va con él.
     * - **El fundido cruzado** —las dos a la vez, que es lo que hace Navigation Compose por defecto
     *   y por tanto el proyecto hermano— pone las dos pantallas a media opacidad a mitad de camino,
     *   y por debajo se ve el fondo del app a través de ambas. Es lo que la primera versión de este
     *   archivo llamó «turbio», y tenía razón.
     *
     * Encendiendo encima no hay ni hueco ni transparencia: la pantalla que llega se materializa
     * sobre la que estaba, que nunca deja de ser opaca. Es lo que el hermano aparenta hacer, hecho
     * de forma que no dependa de que el fondo perdone.
     */
    fun <S> AnimatedContentTransitionScope<S>.siblings(): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(screenFade()),
        // La que se va no se atenúa: aguanta entera y desaparece cuando ya está tapada. Animar su
        // opacidad es lo que dejaba ver el fondo entre las dos.
        initialContentExit = fadeOut(snap(delayMillis = HazloMotion.DURATION_SCREEN_MILLIS)),
        // Encima. Sin esto, «encenderse sobre la otra» depende del orden en que se compongan.
        targetContentZIndex = ARRIVING_ON_TOP,
        // Tambien aqui se apaga el tamaño: dos pilares pueden medir distinto, y sin esto el
        // contenedor interpolaria entre las dos alturas mientras se funden.
        sizeTransform = null,
    )

    /** Lo que se va se apaga rápido, para dejar sitio antes de que llegue lo que viene. */
    private fun <T> leavingFade(): FiniteAnimationSpec<T> =
        tween(HazloMotion.DURATION_FAST_MILLIS, easing = HazloMotion.standard)

    /** Y lo que llega enciende después, sin solaparse con lo que todavía se está yendo. */
    private fun <T> enteringFade(): FiniteAnimationSpec<T> = tween(
        durationMillis = HazloMotion.DURATION_BASE_MILLIS - HazloMotion.DURATION_FAST_MILLIS,
        delayMillis = HazloMotion.DURATION_FAST_MILLIS,
        easing = HazloMotion.standard,
    )

    /** Lo que tarda una pantalla entera en llegar. Ver [HazloMotion.DURATION_SCREEN_MILLIS]. */
    private fun <T> screenFade(): FiniteAnimationSpec<T> =
        tween(HazloMotion.DURATION_SCREEN_MILLIS, easing = HazloMotion.standard)

    private fun <T> slideSpec(): FiniteAnimationSpec<T> =
        tween(HazloMotion.DURATION_BASE_MILLIS, easing = HazloMotion.standard)

    /** Cuánto se retira la pantalla que se va: una cuarta parte, no entera. */
    private const val RECEDING_FRACTION = 4

    /** Por encima de la que se va, que es lo que hace que no se vea el fondo entre las dos. */
    private const val ARRIVING_ON_TOP = 1f
}
