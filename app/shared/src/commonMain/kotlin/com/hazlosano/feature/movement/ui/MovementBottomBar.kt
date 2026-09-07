package com.hazlosano.feature.movement.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.hazlosano.core.ui.model.palette
import com.hazlosano.domain.model.PillarType
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.movement_place_record
import hazlosano.app.shared.generated.resources.outings_title
import hazlosano.app.shared.generated.resources.routes_title
import org.jetbrains.compose.resources.stringResource

/** Los tres sitios del pilar. Un detalle no está aquí: no es un sitio, es algo que abriste. */
enum class MovementPlace {
    Record,
    Routes,
    Outings,
}

/** Etiquetas de prueba: la barra se afirma por aquí y no por su redacción. */
object MovementBottomBarTags {
    const val BAR: String = "movement_bottom_bar"

    fun place(place: MovementPlace): String = "movement_place_${place.name.lowercase()}"
}

/**
 * Por dónde se mueve uno dentro del pilar de Movimiento.
 *
 * **La regla que este componente impone: abajo se va a sitios, en el ⋮ se hacen cosas.** Antes
 * estaban mezclados — el tracker tenía un «Mis salidas» delineado junto al botón de Iniciar (una
 * navegación al lado de una acción), «Mis salidas» tenía una puerta a «Mis rutas» flotando dentro
 * del contenido, y «Mis rutas» no ofrecía ninguna de las otras dos. Moverse por el pilar dependía de
 * por dónde hubieras entrado.
 *
 * **Va en las tres pantallas que son un sitio y en ninguna más.** Los dos detalles —el de una ruta y
 * el de una salida— son algo que abriste *desde* un sitio y de lo que se sale volviendo atrás;
 * ofrecerles destinos hermanos invita a perderse en vez de a volver.
 *
 * **«Grabar» y no «Iniciar».** En el tracker el botón grande ya dice «Iniciar» y hace otra cosa:
 * empezar a grabar. Dos «Iniciar» distintos en la misma pantalla son exactamente la confusión que
 * esta barra existe para quitar, así que el sitio se llama por lo que es y la acción por lo que hace.
 */
@Composable
fun MovementBottomBar(
    current: MovementPlace,
    onGoToRecord: () -> Unit,
    onGoToRoutes: () -> Unit,
    onGoToOutings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = PillarType.MOVEMENT.palette()

    NavigationBar(
        modifier = modifier.testTag(MovementBottomBarTags.BAR),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Place(
            place = MovementPlace.Record,
            current = current,
            label = stringResource(Res.string.movement_place_record),
            icon = Icons.Filled.FiberManualRecord,
            onClick = onGoToRecord,
        )
        Place(
            place = MovementPlace.Routes,
            current = current,
            label = stringResource(Res.string.routes_title),
            icon = Icons.Filled.Map,
            onClick = onGoToRoutes,
        )
        Place(
            place = MovementPlace.Outings,
            current = current,
            label = stringResource(Res.string.outings_title),
            icon = Icons.Filled.Timeline,
            onClick = onGoToOutings,
        )
    }
}

/**
 * Un sitio de la barra.
 *
 * Los papeles del color son dos y no uno, como en la barra de pilares: el indicador se **rellena** y
 * la etiqueta se **escribe**, y el mismo tono no sirve para las dos cosas.
 *
 * Pulsar donde ya estás no hace nada: la pila de navegación corta hasta el destino en vez de apilar
 * una copia, así que sería un viaje a ninguna parte con una recomposición de propina.
 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.Place(
    place: MovementPlace,
    current: MovementPlace,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val palette = PillarType.MOVEMENT.palette()
    val selected = place == current

    NavigationBarItem(
        selected = selected,
        onClick = { if (!selected) onClick() },
        modifier = Modifier.testTag(MovementBottomBarTags.place(place)),
        icon = { Icon(icon, contentDescription = null) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                // Un renglón siempre, como en la barra de pilares: la altura de la barra no puede
                // depender de lo largo que sea una traducción.
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = palette.ink,
            indicatorColor = palette.solid,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
