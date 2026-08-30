package com.hazlosano.core.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.hazlosano.core.ui.theme.PillarMind
import com.hazlosano.core.ui.theme.PillarMovement
import com.hazlosano.core.ui.theme.PillarNutrition
import com.hazlosano.core.ui.theme.PillarSleep
import com.hazlosano.domain.model.PillarType
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.pillar_mind
import hazlosano.app.shared.generated.resources.pillar_movement
import hazlosano.app.shared.generated.resources.pillar_nutrition
import hazlosano.app.shared.generated.resources.pillar_sleep
import org.jetbrains.compose.resources.stringResource

/**
 * Cómo se ve un pilar: su nombre, su color y su icono.
 *
 * Estaba repartido en tres sitios —el color en `PillarColor.kt`, y el nombre y el icono dentro de la
 * feature del catálogo, con una copia privada del icono en Inicio—. Al quererlo una tercera pantalla
 * se promovió aquí, que es lo que su propia nota decía que había que hacer. Fue mover y fusionar, no
 * copiar.
 */

/**
 * El rótulo de un pilar, resuelto del catálogo de cadenas.
 *
 * `PillarType` guarda la **clave** y no el texto porque vive en `core`, que no alcanza los recursos
 * de Compose: una etiqueta escrita allí no se podría traducir nunca. Esta es la mitad de UI de esa
 * separación.
 */
@Composable
fun pillarLabel(pillar: PillarType): String = stringResource(
    when (pillar) {
        PillarType.SLEEP -> Res.string.pillar_sleep
        PillarType.NUTRITION -> Res.string.pillar_nutrition
        PillarType.MOVEMENT -> Res.string.pillar_movement
        PillarType.MIND -> Res.string.pillar_mind
    },
)

/** El color de marca del pilar. Los cuatro ya estaban definidos en el tema. */
fun PillarType.toColor(): Color = when (this) {
    PillarType.SLEEP -> PillarSleep
    PillarType.MOVEMENT -> PillarMovement
    PillarType.NUTRITION -> PillarNutrition
    PillarType.MIND -> PillarMind
}

/**
 * El icono del pilar — los mismos cuatro que ya usa la barra inferior.
 *
 * `BottomTab` sigue llevando los suyos en duro porque también tiene una pestaña que no es un pilar;
 * queda anotado como lo único que falta por unificar de esta familia.
 */
fun pillarIcon(pillar: PillarType): ImageVector = when (pillar) {
    PillarType.SLEEP -> Icons.Filled.Bedtime
    PillarType.NUTRITION -> Icons.Filled.Restaurant
    PillarType.MOVEMENT -> Icons.AutoMirrored.Filled.DirectionsRun
    PillarType.MIND -> Icons.Filled.SelfImprovement
}
