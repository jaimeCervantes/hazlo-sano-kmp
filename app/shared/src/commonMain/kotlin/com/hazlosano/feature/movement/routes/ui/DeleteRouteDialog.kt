package com.hazlosano.feature.movement.routes.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.action_cancel
import hazlosano.app.shared.generated.resources.routes_delete_body
import hazlosano.app.shared.generated.resources.routes_delete_confirm
import hazlosano.app.shared.generated.resources.routes_delete_title
import org.jetbrains.compose.resources.stringResource

/** Etiquetas de prueba: la estructura se afirma por aquí, no por la redacción. */
object DeleteRouteTags {
    const val DIALOG: String = "delete_route_dialog"
    const val CONFIRM: String = "delete_route_confirm"
    const val CANCEL: String = "delete_route_cancel"
}

/**
 * Preguntar antes de borrar una ruta.
 *
 * Borrar era inmediato: un toque en la papelera y la ruta se iba. En una lista donde las filas son
 * altas y los iconos van al final, eso convierte un dedo mal puesto en una ruta perdida — y una ruta
 * importada de un GPX que ya no se tiene no se recupera.
 *
 * **Dice qué se va a borrar, no sólo que algo se va a borrar.** El nombre es lo que deja confirmar
 * sin volver a mirar: si dice otra cosa de la que se tenía en la cabeza, la pregunta ya sirvió.
 *
 * Vive en su propio archivo porque lo usan la lista y el detalle. Estaba a punto de ser lo mismo
 * escrito dos veces.
 */
@Composable
fun DeleteRouteDialog(
    routeName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(DeleteRouteTags.DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.routes_delete_title)) },
        text = { Text(stringResource(Res.string.routes_delete_body, routeName)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(DeleteRouteTags.CONFIRM),
            ) {
                Text(
                    text = stringResource(Res.string.routes_delete_confirm),
                    // Lo destructivo se ve destructivo: es la única acción de estas pantallas que
                    // no se puede deshacer.
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(DeleteRouteTags.CANCEL),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
