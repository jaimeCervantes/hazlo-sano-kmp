package com.hazlosano.feature.movement.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hazlosano.domain.feature.movement.model.UserLocation

/**
 * Platform-specific map surface for the movement pillar, shared by the live tracker and the detail
 * of a recorded session. Android renders a MapLibre map that shows [userLocation] as the current
 * position and draws [path] as a route; other targets render a placeholder.
 *
 * Set [fitPathInView] to frame the whole [path] instead of following the live position — that is
 * what a finished session needs, since it has no current position to follow.
 *
 * [routePath] es la ruta que se está siguiendo, dibujada **debajo** del recorrido real para poder
 * compararlos de un vistazo. Dibujarla no implica seguirla: avisar de un desvío es otra cosa, y es
 * C3 del backlog. La capa ya existía en `MapLayers` desde que se copió de la referencia; lo que
 * faltaba era que alguien la alimentara.
 */
@Composable
expect fun MovementMap(
    userLocation: UserLocation?,
    path: List<UserLocation>,
    modifier: Modifier = Modifier,
    fitPathInView: Boolean = false,
    routePath: List<UserLocation> = emptyList(),
)
