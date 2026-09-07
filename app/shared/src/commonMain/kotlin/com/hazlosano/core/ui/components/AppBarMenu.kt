package com.hazlosano.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: el menú se afirma por aquí y no por su redacción. */
object AppBarMenuTags {
    const val SETTINGS: String = "app_bar_menu_settings"
}

/**
 * Ajustes, en el menú de los tres puntos de cualquier pantalla.
 *
 * **Existe para que Ajustes se alcance sin salir de donde estás.** Antes sólo se llegaba por el ⋮ de
 * la pantalla principal, así que quien estaba grabando una salida y quería cambiar el tema o el
 * idioma tenía que abandonar el pilar entero.
 *
 * Vive aquí y no en `atomic/` porque lee el catálogo, que un componente atómico no puede hacer: se
 * tiene que poder componer desde cualquier sitio, previews y tests incluidos, y allí no hay entorno
 * de recursos. Y vive fuera de la barra porque la barra sí es atómica y no puede escribir «Ajustes»
 * en ningún idioma.
 *
 * Se define una vez y no en cada pantalla: cinco copias de la misma opción son cinco sitios donde la
 * redacción se puede separar.
 *
 * @param afterOtherItems dibuja una raya encima. Va en `true` cuando el menú ya trae acciones sobre
 * lo que se está mirando: lo de la pantalla y lo del app son dos cosas y conviene que se lea que lo
 * son.
 */
@Composable
fun ColumnScope.AppSettingsMenuItem(
    onClick: () -> Unit,
    afterOtherItems: Boolean = false,
) {
    if (afterOtherItems) {
        HorizontalDivider()
    }
    DropdownMenuItem(
        text = { Text(stringResource(Res.string.settings_title)) },
        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
        onClick = onClick,
        modifier = Modifier.testTag(AppBarMenuTags.SETTINGS),
    )
}
