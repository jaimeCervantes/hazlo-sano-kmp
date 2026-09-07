package com.hazlosano.feature.movement.detail.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.components.AppBarMenuTags
import com.hazlosano.core.ui.components.atomic.HazloTopAppBar
import com.hazlosano.core.ui.components.atomic.HazloTopAppBarTags
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El menú del detalle de una salida, con la forma que este pilar ya usa en todas partes: **lo que se
 * le hace a lo que estás mirando, una raya, y lo del app.**
 *
 * «Guardar como ruta» vivía suelto bajo la barra, dentro del contenido. Es una acción sobre la salida
 * que se está mirando, así que va donde van las acciones desde el slice 3 — igual que descargar y
 * borrar en el detalle de una ruta.
 *
 * La pantalla entera no se puede componer en la JVM: construye su ViewModel con la persistencia real.
 * Lo que se afirma aquí es la **forma del menú**, que es lo que este cambio decide.
 *
 * Spec: `features/pulido_de_movimiento.feature`, slice 5.
 */
@OptIn(ExperimentalTestApi::class)
class SessionDetailMenuTest {

    @Test
    fun `a session that can become a route offers it in the menu`() = runComposeUiTest {
        var saved = 0
        setContent {
            HazloTopAppBar(
                title = "Vuelta a la manzana",
                menuContentDescription = "Menú",
                menuContent = { dismiss ->
                    SessionDetailMenuItems(
                        canSaveAsRoute = true,
                        onSaveAsRoute = {
                            dismiss()
                            saved++
                        },
                        onOpenSettings = {},
                    )
                },
            )
        }

        onNodeWithTag(HazloTopAppBarTags.MENU).performClick()
        onNodeWithTag(SessionDetailTags.SAVE_AS_ROUTE).performClick()

        assertEquals(1, saved)
    }

    /**
     * Una salida que no puede ser ruta no lo ofrece, pero Ajustes sigue estando.
     *
     * Es la misma distinción que en el detalle de una ruta: lo de esto que miras depende de que haya
     * algo que mirar; lo del app, no.
     */
    @Test
    fun `a session that cannot become a route still reaches settings`() = runComposeUiTest {
        var openedSettings = 0
        setContent {
            HazloTopAppBar(
                title = "Vuelta a la manzana",
                menuContentDescription = "Menú",
                menuContent = { dismiss ->
                    SessionDetailMenuItems(
                        canSaveAsRoute = false,
                        onSaveAsRoute = {},
                        onOpenSettings = {
                            dismiss()
                            openedSettings++
                        },
                    )
                },
            )
        }

        onNodeWithTag(HazloTopAppBarTags.MENU).performClick()

        onNodeWithTag(SessionDetailTags.SAVE_AS_ROUTE).assertDoesNotExist()
        onNodeWithTag(AppBarMenuTags.SETTINGS).assertIsDisplayed()
        onNodeWithTag(AppBarMenuTags.SETTINGS).performClick()
        assertEquals(1, openedSettings)
    }
}
