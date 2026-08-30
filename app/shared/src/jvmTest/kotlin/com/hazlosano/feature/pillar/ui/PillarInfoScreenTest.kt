package com.hazlosano.feature.pillar.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.domain.model.PillarType
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Qué es un pilar, leído como lo lee quien abre la pantalla.
 *
 * Las afirmaciones van sobre el texto porque **el texto es la funcionalidad**: esta pantalla no hace
 * nada más que contar de qué va el pilar. Si la redacción cambia en el sitio, esta prueba debe
 * cambiar con ella; es justo lo que se quiere vigilar.
 */
@OptIn(ExperimentalTestApi::class)
class PillarInfoScreenTest {

    @Test
    fun `the movement pillar says what it is and what it proposes`() = runComposeUiTest {
        setContent { PillarInfoScreen(pillar = PillarType.MOVEMENT, onBack = {}) }

        onNodeWithText("Movimiento natural, local y comunitario").assertIsDisplayed()
        onNodeWithText("Recuperar el cuerpo en la calle, el sendero y la cancha del barrio.")
            .assertIsDisplayed()
        onNodeWithText("Movimiento vivo, local y funcional").assertIsDisplayed()
    }

    @Test
    fun `the sleep pillar says what it is and what it proposes`() = runComposeUiTest {
        setContent { PillarInfoScreen(pillar = PillarType.SLEEP, onBack = {}) }

        onNodeWithText("Sueño y Descanso").assertIsDisplayed()
        onNodeWithText("Del atardecer al amanecer").assertIsDisplayed()
    }

    @Test
    fun `the nutrition pillar says what it is and what it proposes`() = runComposeUiTest {
        setContent { PillarInfoScreen(pillar = PillarType.NUTRITION, onBack = {}) }

        onNodeWithText("Alimentación natural, nutritiva y local").assertIsDisplayed()
        onNodeWithText("Cena real, local y al atardecer").assertIsDisplayed()
    }

    @Test
    fun `the mind pillar says what it is and what it proposes`() = runComposeUiTest {
        setContent { PillarInfoScreen(pillar = PillarType.MIND, onBack = {}) }

        onNodeWithText("Mente, espíritu y comunidad cercana").assertIsDisplayed()
        onNodeWithText("Presencia, paz y conexión local").assertIsDisplayed()
    }

    @Test
    fun `every pillar carries a hero, a body and a practice`() = runComposeUiTest {
        setContent { PillarInfoScreen(pillar = PillarType.MIND, onBack = {}) }

        onNodeWithTag(PillarInfoTags.HERO).assertIsDisplayed()
        onNodeWithTag(PillarInfoTags.BODY).assertIsDisplayed()
        onNodeWithTag(PillarInfoTags.PRACTICE).assertIsDisplayed()
    }

    @Test
    fun `going back returns to the pillar it was opened from`() = runComposeUiTest {
        var wentBack = false

        setContent {
            PillarInfoScreen(pillar = PillarType.NUTRITION, onBack = { wentBack = true })
        }

        onNodeWithContentDescription("Volver").performClick()

        assertTrue(wentBack, "el botón de volver devuelve al tablero")
    }
}
