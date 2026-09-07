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
    const val PROFILE: String = "hazlo_top_app_bar_profile"
    const val NOTIFICATIONS: String = "hazlo_top_app_bar_notifications"
    const val MENU: String = "hazlo_top_app_bar_menu"
}

/**
 * La barra superior del app.
 *
 * **Un control se pinta porque alguien le dio algo que hacer.** Las acciones son nulables y su valor
 * por defecto es `null`, no `{}`: con la lambda vacia por defecto los tres iconos se pintaban
 * siempre, y en siete de las ocho pantallas ninguno estaba conectado — se pintaban, respondian al
 * toque con su ondita y no pasaba nada. Un control que miente sobre lo que hace enseña a desconfiar
 * de la interfaz entera, que es el mismo principio que este pilar aplica a sus cifras: si no lo
 * sabes, no lo digas.
 *
 * Que el tipo sea nulable es lo que hace la regla comprobable en vez de una intencion: no se puede
 * pintar un boton sin tener a quien llamar.
 */
@Composable
fun HazloTopAppBar(
    title: String = "Hazlo Sano",
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onProfileClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    backContentDescription: String? = null,
    profileContentDescription: String? = null,
    notificationsContentDescription: String? = null,
    menuContentDescription: String? = null,
    menuContent: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // Los tres puntos valen para las dos formas: un menu desplegable, o una accion suelta. Se pintan
    // si hay cualquiera de las dos.
    val hasMenu = menuContent != null || onMenuClick != null
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
        } else if (onProfileClick != null) {
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(40.dp).testTag(HazloTopAppBarTags.PROFILE),
            ) {
                Icon(
                    imageVector = leadingIcon ?: Icons.Filled.Person,
                    contentDescription = profileContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Sin vuelta atras y sin perfil no se pinta nada a la izquierda, y el titulo se va al
        // principio. Es lo que pasa hoy en la pantalla principal: el muñeco del perfil llevaba
        // desde siempre sin llevar a ningun sitio, y no hay perfil al que llevar todavia.
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row {
            if (onNotificationsClick != null) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(40.dp).testTag(HazloTopAppBarTags.NOTIFICATIONS),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = notificationsContentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (hasMenu) {
                Box {
                    IconButton(
                        onClick = {
                            if (menuContent != null) menuOpen = true else onMenuClick?.invoke()
                        },
                        modifier = Modifier.size(40.dp).testTag(HazloTopAppBarTags.MENU),
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
                            // acordarse, la que se olvidara dejaria el menu abierto sobre la
                            // pantalla que acaba de cambiar.
                            menuContent { menuOpen = false }
                        }
                    }
                }
            }
        }
    }
}
