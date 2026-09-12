package com.hazlosano.feature.movement.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hazlosano.core.ui.components.AppSettingsMenuItem
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.theme.HazloTransitions
import com.hazlosano.feature.movement.detail.ui.SessionDetailScreen
import com.hazlosano.feature.movement.history.ui.MovementHistoryScreen
import com.hazlosano.feature.movement.presentation.MovementDestination
import com.hazlosano.feature.movement.presentation.MovementNavState
import com.hazlosano.feature.movement.presentation.MovementPlace
import com.hazlosano.feature.movement.presentation.movementPlaceOf
import com.hazlosano.feature.movement.routes.presentation.RoutesViewModel
import com.hazlosano.feature.movement.routes.ui.RouteDetailScreen
import com.hazlosano.feature.movement.routes.ui.RoutesScreen
import com.hazlosano.feature.movement.tracker.ui.TrackerScreen
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.outings_title
import hazlosano.app.shared.generated.resources.routes_title
import hazlosano.app.shared.generated.resources.top_app_bar_back
import hazlosano.app.shared.generated.resources.tracker_title
import hazlosano.app.shared.generated.resources.top_app_bar_menu
import org.jetbrains.compose.resources.stringResource

/**
 * El pilar de Movimiento entero: sus cinco pantallas y cómo se pasa de una a otra.
 *
 * **Son dos niveles y no uno, y de ahí sale que se sienta fluido.**
 *
 * - Fuera, [MovementSection] distingue «los tres sitios» de «un detalle». Sólo cambia al abrir o
 *   cerrar un detalle, y eso sí es entrar y salir: se desliza.
 * - Dentro de los tres sitios, el cromo —las dos barras— se pinta **una vez, fuera** de la animación,
 *   y sólo se funde el cuerpo.
 *
 * Antes era un solo `AnimatedContent` sobre los cinco destinos, así que cambiar de sitio animaba
 * también las barras: al ir de «Mis rutas» a «Mis salidas» parpadeaban la de arriba y la de abajo
 * además del contenido. Es exactamente lo que las cinco pestañas de pilar no hacen —su
 * `NavigationBar` vive fuera del `AnimatedContent` de `MainScreen`— y por eso aquéllas se sentían
 * bien y éstas no.
 *
 * Con los dos niveles, la regla de qué transición toca deja de ser una función aparte y pasa a estar
 * en la forma: **entre hermanos no hay nada que animar salvo el cuerpo, porque los hermanos viven
 * dentro del mismo cromo.**
 *
 * Vive aquí y no en `MainScreen` porque es navegación del pilar, no de la pantalla principal: allí
 * eran ochenta líneas que obligaban a `MainScreen` a conocer los cinco destinos y sus argumentos.
 */
@Composable
fun MovementPillar(
    nav: MovementNavState,
    routesViewModel: RoutesViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // `lastPlace` y no `destination`: al cerrar el pilar la pila se vacía de inmediato, pero esta
    // pantalla sigue compuesta hasta que termina de irse. Sin esto se vería un panel en blanco
    // deslizándose en lugar de la pantalla de la que se viene.
    val here = nav.lastPlace

    AnimatedContent(
        // **Sin la profundidad de la pila, y eso es el slice entero.** Ir de «Mis rutas» a «Mis
        // salidas» hace crecer o menguar la pila, así que con la profundidad dentro del estado este
        // nivel cambiaría también al cambiar de sitio — y volvería a animar el cromo, que es
        // exactamente lo que esto viene a quitar. `Places` es un `data object`: pasar de un sitio a
        // otro no lo mueve, y aquí no ocurre nada.
        targetState = sectionOf(here),
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            // Y por eso la dirección tampoco sale de la pila: este nivel sólo va de los sitios a un
            // detalle y al revés. Hacia un detalle se entra; hacia los sitios se vuelve.
            with(HazloTransitions) {
                depth(goingDeeper = targetState is MovementSection.Detail)
            }
        },
        label = "pilar de movimiento",
    ) { section ->
        when (section) {
            MovementSection.Places -> Places(
                // `lastVisitedPlace` y no `here`: al abrir un detalle, `here` ya es ese detalle, y
                // esta mitad —que sigue compuesta mientras se desliza hacia fuera— se quedaría
                // pintando nada.
                destination = nav.lastVisitedPlace,
                nav = nav,
                routesViewModel = routesViewModel,
                onOpenSettings = onOpenSettings,
            )

            is MovementSection.Detail -> Detail(
                destination = section.destination,
                nav = nav,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

/**
 * Los tres sitios, bajo un cromo que no se mueve.
 *
 * El título cambia de golpe y el cuerpo se funde. Es a propósito: la barra tiene que quedarse
 * quieta, y fundir el título contra sí mismo dejaría dos textos superpuestos medio segundo — que es
 * justo lo que se veía cuando las barras estaban dentro de la animación.
 */
@Composable
private fun Places(
    destination: MovementDestination,
    nav: MovementNavState,
    routesViewModel: RoutesViewModel,
    onOpenSettings: () -> Unit,
) {
    val place = movementPlaceOf(destination) ?: return

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HazloTopAppBar(
            title = stringResource(place.titleResource()),
            showBackButton = true,
            onBackClick = { nav.back() },
            backContentDescription = stringResource(Res.string.top_app_bar_back),
            menuContentDescription = stringResource(Res.string.top_app_bar_menu),
            menuContent = { dismiss ->
                AppSettingsMenuItem(onClick = { dismiss(); onOpenSettings() })
            },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = destination,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { with(HazloTransitions) { siblings() } },
                label = "sitio del pilar",
            ) { current ->
                when (current) {
                    is MovementDestination.Tracker -> TrackerScreen(
                        followRouteId = current.followRouteId,
                        modifier = Modifier.fillMaxSize(),
                    )

                    MovementDestination.Routes -> RoutesScreen(
                        viewModel = routesViewModel,
                        onOpenRoute = { id -> nav.openRouteDetail(id) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    MovementDestination.History -> MovementHistoryScreen(
                        onOpenSession = { id -> nav.openSessionDetail(id) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Un detalle nunca llega aquí: lo separa el nivel de fuera.
                    else -> Unit
                }
            }
        }

        MovementBottomBar(
            current = place,
            onGoToRecord = { nav.openTracker() },
            onGoToRoutes = { nav.openRoutes() },
            onGoToOutings = { nav.openHistory() },
        )
    }
}

/**
 * Un detalle, con su propio cromo.
 *
 * **No comparte la barra de arriba, y no es por descuido:** su título sale de sus propios datos — el
 * nombre de la ruta, la fecha de la salida—, que el cromo de fuera no tiene ni debería ir a buscar.
 * Y barra de abajo no lleva ninguno, que es lo que `MovementBottomBar` ya dice de sí misma.
 */
@Composable
private fun Detail(
    destination: MovementDestination,
    nav: MovementNavState,
    onOpenSettings: () -> Unit,
) {
    when (destination) {
        is MovementDestination.RouteDetail -> RouteDetailScreen(
            routeId = destination.routeId,
            onBack = { nav.back() },
            onStartOuting = { nav.openTrackerFollowing(destination.routeId) },
            onOpenSettings = onOpenSettings,
            modifier = Modifier.fillMaxSize(),
        )

        is MovementDestination.SessionDetail -> SessionDetailScreen(
            sessionId = destination.sessionId,
            onBack = { nav.back() },
            onOpenSettings = onOpenSettings,
            modifier = Modifier.fillMaxSize(),
        )

        else -> Unit
    }
}

/**
 * Qué mitad del pilar se está viendo.
 *
 * [Places] no lleva cuál de los tres: ése es el punto. Si lo llevara, cambiar de sitio cambiaría el
 * estado de fuera y volvería a animar el cromo, que es el fallo que esto viene a quitar.
 */
private sealed interface MovementSection {
    data object Places : MovementSection
    data class Detail(val destination: MovementDestination) : MovementSection
}

private fun sectionOf(destination: MovementDestination): MovementSection =
    if (movementPlaceOf(destination) != null) {
        MovementSection.Places
    } else {
        MovementSection.Detail(destination)
    }

/**
 * Lo que dice la barra de arriba en cada sitio.
 *
 * Grabar es el único que no se llama igual arriba que abajo: arriba dice «Movimiento», el nombre del
 * pilar, y abajo «Grabar», que es lo que se hace ahí. Se conserva tal cual — cambiarlo sería
 * reescribir la interfaz desde un slice que vino a mover una barra de sitio.
 */
internal fun MovementPlace.titleResource() = when (this) {
    MovementPlace.Record -> Res.string.tracker_title
    MovementPlace.Routes -> Res.string.routes_title
    MovementPlace.Outings -> Res.string.outings_title
}
