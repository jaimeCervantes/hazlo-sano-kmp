package com.hazlosano.feature.movement.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.theme.HazloShapes
import com.hazlosano.core.ui.theme.HazloSpaces
import com.hazlosano.domain.model.PillarType
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.movement_start_outing
import hazlosano.app.shared.generated.resources.outings_title
import hazlosano.app.shared.generated.resources.routes_title
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: las puertas se afirman por aquí y no por su redacción. */
object MovementPillarActionTags {
    const val START: String = "movement_action_start"
    const val ROUTES: String = "movement_action_routes"
    const val OUTINGS: String = "movement_action_outings"
}

/**
 * Las herramientas del pilar de Movimiento, en su propia pestaña.
 *
 * Existe porque hasta ahora la pestaña del pilar sólo enseñaba lo publicado: para grabar una salida
 * había que salir a Inicio, y para abrir una ruta había que atravesar el tracker y el historial.
 *
 * **Una acción manda y dos acompañan.** Empezar una salida es a lo que se viene, así que va rellena y
 * a lo ancho; las dos puertas son lugares a los que ir, no cosas que hacer ahora, y van delineadas.
 * Es la jerarquía a la que han convergido las apps del ramo, y la misma que el resto del app usa
 * entre su botón primario y sus `TextButton`.
 */
@Composable
fun MovementPillarActions(
    onStartOuting: () -> Unit,
    onOpenRoutes: () -> Unit,
    onOpenOutings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = PillarType.MOVEMENT.palette()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HazloSpaces.gutter, vertical = HazloSpaces.sm),
        verticalArrangement = Arrangement.spacedBy(HazloSpaces.sm),
    ) {
        Button(
            onClick = onStartOuting,
            modifier = Modifier.fillMaxWidth().testTag(MovementPillarActionTags.START),
            shape = RoundedCornerShape(HazloShapes.control),
            colors = ButtonDefaults.buttonColors(containerColor = palette.solid),
        ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null)
            Text(
                text = stringResource(Res.string.movement_start_outing),
                modifier = Modifier.padding(start = HazloSpaces.sm),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(HazloSpaces.sm)) {
            SecondaryDoor(
                label = stringResource(Res.string.routes_title),
                icon = Icons.Filled.Map,
                tag = MovementPillarActionTags.ROUTES,
                onClick = onOpenRoutes,
                modifier = Modifier.weight(1f),
            )
            SecondaryDoor(
                label = stringResource(Res.string.outings_title),
                icon = Icons.Filled.Timeline,
                tag = MovementPillarActionTags.OUTINGS,
                onClick = onOpenOutings,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SecondaryDoor(
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = PillarType.MOVEMENT.palette()

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.testTag(tag),
        shape = RoundedCornerShape(HazloShapes.control),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.ink),
    ) {
        Icon(icon, contentDescription = null)
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            // Las dos puertas se reparten el ancho a medias, así que ninguna puede crecer a costa de
            // la otra ni partir en dos renglones y dejarlas de distinta altura.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = HazloSpaces.xs),
        )
    }
}
