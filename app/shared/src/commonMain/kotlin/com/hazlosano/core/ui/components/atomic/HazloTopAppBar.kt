package com.hazlosano.core.ui.components.atomic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazlosano.core.ui.theme.HazloSpaces

/** Etiquetas de prueba: la barra se afirma por aqui y no por su redaccion. */
object HazloTopAppBarTags {
    const val BAR: String = "hazlo_top_app_bar"
}

@Composable
fun HazloTopAppBar(
    title: String = "Hazlo Sano",
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    leadingIcon: ImageVector? = null,
    backContentDescription: String? = null,
    profileContentDescription: String? = null,
    notificationsContentDescription: String? = null,
    menuContentDescription: String? = null,
    menuContent: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // Quien pasa [menuContent] convierte los tres puntos en un menu de verdad; quien no, se queda
    // con el callback suelto de siempre. La barra no sabe que hay dentro del menu: recibe el hueco
    // ya escrito, que es lo que le permite seguir sin conocer ni dominio ni recursos.
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // La barra pinta su propio fondo, y no lo hereda de quien la componga.
            //
            // Era transparente, y las ocho pantallas que la usan repetian a mano la misma linea de
            // fondo en su raiz. Siete se acordaron; `RouteDetailScreen` no, y era la unica pantalla
            // del app cuyo header no respetaba el tema. El fallo no estaba en esa pantalla: estaba
            // en que la barra dejaba el trabajo a quien la llamara, con ocho ocasiones de olvidarlo
            // y una mas por cada pantalla nueva.
            //
            // Se pinta ANTES del padding de la barra de estado, para que el color llegue tambien
            // bajo el reloj y la bateria en vez de dejar una franja del color del sistema.
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = HazloSpaces.gutter)
            .padding(top = HazloSpaces.unit)
            .testTag(HazloTopAppBarTags.BAR),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBackButton) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (leadingIcon != null) {
            IconButton(onClick = onProfileClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = profileContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            IconButton(onClick = onProfileClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = profileContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = notificationsContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(
                    onClick = { if (menuContent != null) menuOpen = true else onMenuClick() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = menuContentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (menuContent != null) {
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        // Cerrar es cosa del menu y no de cada opcion: si cada una tuviera que
                        // acordarse, la que se olvidara dejaria el menu abierto sobre la pantalla
                        // que acaba de cambiar.
                        menuContent { menuOpen = false }
                    }
                }
            }
        }
    }
}
