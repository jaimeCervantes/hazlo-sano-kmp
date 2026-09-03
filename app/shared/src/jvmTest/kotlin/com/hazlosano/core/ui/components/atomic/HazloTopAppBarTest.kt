package com.hazlosano.core.ui.components.atomic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

/**
 * `HazloTopAppBar` es un componente atómico: no puede leer el catálogo de recursos ni llevar copia
 * escrita dentro. Cada descripción de accesibilidad entra por parámetro, y lo que se comprueba aquí
 * es que el icono expone exactamente el valor que recibe, sin importar quién lo redacte.
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
            HazloTopAppBar(showBackButton = false, profileContentDescription = "Perfil")
        }

        onNodeWithContentDescription("Perfil").assertIsDisplayed()
    }

    @Test
    fun `a leading icon still resolves the profile description`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(
                showBackButton = false,
                leadingIcon = Icons.Filled.Person,
                profileContentDescription = "Perfil",
            )
        }

        onNodeWithContentDescription("Perfil").assertIsDisplayed()
    }

    @Test
    fun `the notifications icon exposes the description it is given`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(notificationsContentDescription = "Notificaciones")
        }

        onNodeWithContentDescription("Notificaciones").assertIsDisplayed()
    }

    @Test
    fun `the menu icon exposes the description it is given`() = runComposeUiTest {
        setContent {
            HazloTopAppBar(menuContentDescription = "Menú")
        }

        onNodeWithContentDescription("Menú").assertIsDisplayed()
    }
}
