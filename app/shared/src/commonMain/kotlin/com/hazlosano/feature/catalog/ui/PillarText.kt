package com.hazlosano.feature.catalog.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
 * El rótulo de un pilar, resuelto del catálogo de cadenas.
 *
 * `PillarType` guarda la **clave** y no el texto porque vive en `core`, que no alcanza los recursos
 * de Compose: una etiqueta escrita allí no se podría traducir nunca. Esta es la mitad de UI de esa
 * separación.
 *
 * Vive dentro de la feature del catálogo porque hoy solo la usa ella. En cuanto una segunda
 * pantalla la quiera —`MainScreen` lleva los cuatro nombres en duro en `BottomTab`, que es deuda
 * anotada— se promueve a `core/ui/`, moviéndola y sin dejar copia.
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
fun pillarColor(pillar: PillarType): Color = when (pillar) {
    PillarType.SLEEP -> PillarSleep
    PillarType.NUTRITION -> PillarNutrition
    PillarType.MOVEMENT -> PillarMovement
    PillarType.MIND -> PillarMind
}
