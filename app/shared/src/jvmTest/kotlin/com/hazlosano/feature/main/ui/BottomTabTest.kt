package com.hazlosano.feature.main.ui

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.model.palette
import com.hazlosano.core.ui.model.pillarIcon
import com.hazlosano.core.ui.model.pillarLabel
import com.hazlosano.core.ui.theme.PillarPalette
import com.hazlosano.domain.model.PillarType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `BottomTab` llevaba su propia copia del nombre, el icono y el color de cada pilar. Lo que se
 * comprueba aquí es que una pestaña de pilar se ve exactamente igual que el tablero al que lleva,
 * porque las dos leen ahora de `PillarVisuals`.
 */
@OptIn(ExperimentalTestApi::class)
class BottomTabTest {

    private class Captured(val label: String, val icon: ImageVector, val palette: PillarPalette)

    private fun assertMatchesPillar(tab: BottomTab, pillar: PillarType) = runComposeUiTest {
        lateinit var actual: Captured
        lateinit var expected: Captured
        setContent {
            actual = Captured(tab.accessibleLabel(), tab.icon(), tab.palette())
            expected = Captured(pillarLabel(pillar), pillarIcon(pillar), pillar.palette())
        }

        // Se compara contra el nombre **entero**, que es el que la pestaña sigue anunciando a un
        // lector de pantalla. Lo que se pinta puede ser más corto; ver `theMindTabFitsOnOneLine`.
        assertEquals(expected.label, actual.label)
        assertEquals(expected.icon, actual.icon)
        // Los tres papeles, no sólo uno: la pestaña rellena su indicador con `solid` y escribe su
        // etiqueta con `ink`, así que comparar un color suelto ya no diría si coinciden.
        assertEquals(expected.palette, actual.palette)
    }

    @Test
    fun `the sleep tab matches the sleep pillar's visuals`() =
        assertMatchesPillar(BottomTab.Sueno, PillarType.SLEEP)

    @Test
    fun `the nutrition tab matches the nutrition pillar's visuals`() =
        assertMatchesPillar(BottomTab.Nutricion, PillarType.NUTRITION)

    @Test
    fun `the movement tab matches the movement pillar's visuals`() =
        assertMatchesPillar(BottomTab.Movimiento, PillarType.MOVEMENT)

    @Test
    fun `the mind tab matches the mind pillar's visuals`() =
        assertMatchesPillar(BottomTab.Mente, PillarType.MIND)

    @Test
    fun `the home tab carries no pillar`() {
        assertEquals(null, BottomTab.Inicio.pillar)
    }

    /**
     * "Mente y Espíritu" no cabe en una pestaña de cinco: partía en dos renglones y dejaba esa
     * pestaña más alta que las otras cuatro, con su indicador desalineado. La barra enseña la forma
     * corta y sigue anunciando el nombre entero.
     */
    @Test
    fun `the mind tab shows a short label and still announces the full one`() = runComposeUiTest {
        lateinit var shown: String
        lateinit var announced: String
        setContent {
            shown = BottomTab.Mente.label()
            announced = BottomTab.Mente.accessibleLabel()
        }

        assertEquals("Mente", shown)
        assertEquals("Mente y Espíritu", announced)
    }

    /**
     * Ninguna pestaña puede llevar un rótulo que no quepa en un renglón. El número sale de la más
     * larga que hoy entra bien —"Alimentación", 12 caracteres—; pasarse de ahí es lo que rompió la
     * barra, así que se fija como límite en vez de descubrirlo otra vez en una captura.
     */
    @Test
    fun `no tab label is longer than the widest one that fits`() = runComposeUiTest {
        lateinit var labels: List<String>
        setContent { labels = BottomTab.entries.map { it.label() } }

        for (label in labels) {
            assertTrue(
                label.length <= 12,
                "El rótulo \"$label\" tiene ${label.length} caracteres y parte en dos renglones " +
                    "en una barra de cinco pestañas.",
            )
        }
    }
}
