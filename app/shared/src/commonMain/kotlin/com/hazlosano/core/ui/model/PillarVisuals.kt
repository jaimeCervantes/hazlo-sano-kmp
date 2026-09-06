package com.hazlosano.core.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.hazlosano.core.ui.theme.DarkHazloPalette
import com.hazlosano.core.ui.theme.HazloPalette
import com.hazlosano.core.ui.theme.LocalHazloPalette
import com.hazlosano.core.ui.theme.PillarPalette
import com.hazlosano.domain.model.PillarType
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.pillar_mind
import hazlosano.app.shared.generated.resources.pillar_mind_short
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

/**
 * El rótulo del pilar para sitios estrechos.
 *
 * Sólo Mente y Espíritu tiene forma corta, porque es el único cuyo nombre son tres palabras: en la
 * barra inferior, con cinco pestañas, partía en dos renglones y dejaba esa pestaña más alta que las
 * otras cuatro. Los demás devuelven su nombre completo, así que quien necesite la versión compacta
 * puede pedirla siempre sin preguntar de qué pilar se trata.
 */
@Composable
fun pillarShortLabel(pillar: PillarType): String = when (pillar) {
    PillarType.MIND -> stringResource(Res.string.pillar_mind_short)
    else -> pillarLabel(pillar)
}

/**
 * Los tres papeles del pilar: relleno, papel tenue y tinta.
 *
 * Sustituye al antiguo `toColor()`, que devolvía **un** color para los dos trabajos. Con un solo
 * tono la elección correcta no existía: nueve pantallas escribían cifras con el color de relleno del
 * pilar, y tres de los cuatro rellenos no llegaban a AA como tinta. Devolver la paleta obliga a
 * decir para qué se quiere el color, que es lo que hace visible el error.
 */
@Composable
fun PillarType.palette(): PillarPalette = LocalHazloPalette.current.of(this)

/** La misma resolución, fuera de una composición — para tests y para previews. */
fun HazloPalette.of(pillar: PillarType): PillarPalette = when (pillar) {
    PillarType.SLEEP -> sleep
    PillarType.MOVEMENT -> movement
    PillarType.NUTRITION -> nutrition
    PillarType.MIND -> mind
}

/**
 * La tinta de un pilar cuando va **encima de una foto o de un velo negro**.
 *
 * No depende del tema, y eso es deliberado: la tarjeta de un pilar en Inicio, la de una noche y el
 * resumen de sueño pintan sobre una imagen oscurecida, así que su fondo es oscuro también cuando el
 * app está en claro. Con la tinta del tema activo, en modo claro el texto quedaría en el verde
 * oscuro (#3C7B0F) sobre negro — ilegible. Sobre foto, el suelo siempre es oscuro, así que la tinta
 * siempre es la del tema oscuro.
 */
fun pillarInkOnImage(pillar: PillarType): Color = DarkHazloPalette.of(pillar).ink

/**
 * El icono del pilar — los mismos cuatro que usa la barra inferior, que los lee de aquí desde que
 * dejó de llevar su propia copia.
 */
fun pillarIcon(pillar: PillarType): ImageVector = when (pillar) {
    PillarType.SLEEP -> Icons.Filled.Bedtime
    PillarType.NUTRITION -> Icons.Filled.Restaurant
    PillarType.MOVEMENT -> Icons.AutoMirrored.Filled.DirectionsRun
    PillarType.MIND -> Icons.Filled.SelfImprovement
}
