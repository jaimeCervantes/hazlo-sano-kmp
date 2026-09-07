package com.hazlosano.core.ui.components.atomic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * `HazloTopAppBar` es un componente atómico: no puede leer el catálogo de recursos ni llevar copia
 * escrita dentro. Cada descripción de accesibilidad entra por parámetro, y lo que se comprueba aquí
 * es que el icono expone exactamente el valor que recibe, sin importar quién lo redacte.
 *
 * **Y que se pinta sólo si tiene algo que hacer.** Las acciones son nulables y por defecto `null`:
 * con la lambda vacía de antes los tres iconos aparecían siempre, y en siete de las ocho pantallas
 * ninguno estaba conectado — se pintaban, respondían al toque y no pasaba nada. Los dos lados de esa
 * regla se afirman abajo, porque el que importa es el negativo: probar sólo que un control con
 * acción aparece dejaría pasar exactamente el fallo que había.
 */
@OptIn(ExperimentalTestApi::class)
class HazloTopAppBarTest {

    @Test
    fun `the back icon exposes the description it is given`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(showBackButton = true, backContentDescription = "Volver")
        }

        onNodeWithContentDescription("Volver").assertIsDisplayed()
    }

    @Test
    fun `the profile icon exposes the description it is given`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(
                showBackButton = false,
                onProfileClick = {},
                profileContentDescription = "Perfil",
            )
        }

        onNodeWithContentDescription("Perfil").assertIsDisplayed()
    }

    @Test
    fun `a leading icon still resolves the profile description`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(
                showBackButton = false,
                onProfileClick = {},
                leadingIcon = Icons.Filled.Person,
                profileContentDescription = "Perfil",
            )
        }

        onNodeWithContentDescription("Perfil").assertIsDisplayed()
    }

    @Test
    fun `the notifications icon exposes the description it is given`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(
                onNotificationsClick = {},
                notificationsContentDescription = "Notificaciones",
            )
        }

        onNodeWithContentDescription("Notificaciones").assertIsDisplayed()
    }

    @Test
    fun `the menu icon exposes the description it is given`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(onMenuClick = {}, menuContentDescription = "Menú")
        }

        onNodeWithContentDescription("Menú").assertIsDisplayed()
    }

    // ─────────── La regla: un control se pinta porque tiene algo que hacer ───────────

    /**
     * El caso que este slice existe para arreglar: una barra a la que nadie le dio nada que hacer no
     * pinta un solo control, en vez de tres que responden al toque y no llevan a ningún sitio.
     */
    @Test
    fun `a bar with nothing to do paints no controls at all`() = runComposeUiTest {
        setContent { HazloTopAppBar(title = "Mis rutas") }

        onNodeWithTag(HazloTopAppBarTags.PROFILE).assertDoesNotExist()
        onNodeWithTag(HazloTopAppBarTags.NOTIFICATIONS).assertDoesNotExist()
        onNodeWithTag(HazloTopAppBarTags.MENU).assertDoesNotExist()
    }

    @Test
    fun `each control appears only when it is given an action`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(title = "Mis rutas", onNotificationsClick = {})
        }

        onNodeWithTag(HazloTopAppBarTags.NOTIFICATIONS).assertIsDisplayed()
        onNodeWithTag(HazloTopAppBarTags.PROFILE).assertDoesNotExist()
        onNodeWithTag(HazloTopAppBarTags.MENU).assertDoesNotExist()
    }

    /** Los tres puntos valen para las dos formas, y un menú desplegable también los enciende. */
    @Test
    fun `a dropdown menu is reason enough to paint the three dots`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(title = "Mis rutas", menuContent = { _ -> })
        }

        onNodeWithTag(HazloTopAppBarTags.MENU).assertIsDisplayed()
    }

    /** Con la vuelta atrás puesta, el sitio de la izquierda es suyo y el perfil no compite. */
    @Test
    fun `going back takes the left slot even when there is a profile action`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(
                showBackButton = true,
                onBackClick = {},
                onProfileClick = {},
                backContentDescription = "Volver",
            )
        }

        onNodeWithContentDescription("Volver").assertIsDisplayed()
        onNodeWithTag(HazloTopAppBarTags.PROFILE).assertDoesNotExist()
    }
}
