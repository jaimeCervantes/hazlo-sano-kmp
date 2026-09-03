package com.hazlosano.feature.main.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.model.pillarIcon
import com.hazlosano.core.ui.model.pillarLabel
import com.hazlosano.core.ui.model.toColor
import com.hazlosano.domain.model.PillarType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `BottomTab` llevaba su propia copia del nombre, el icono y el color de cada pilar. Lo que se
 * comprueba aquí es que una pestaña de pilar se ve exactamente igual que el tablero al que lleva,
 * porque las dos leen ahora de `PillarVisuals`.
 */
@OptIn(ExperimentalTestApi::class)
class BottomTabTest {

    private class Captured(val label: String, val icon: ImageVector, val color: Color)

    private fun assertMatchesPillar(tab: BottomTab, pillar: PillarType) = runComposeUiTest {
        lateinit var actual: Captured
        lateinit var expected: Captured
        setContent {
            actual = Captured(tab.label(), tab.icon(), tab.color())
            expected = Captured(pillarLabel(pillar), pillarIcon(pillar), pillar.toColor())
        }

        assertEquals(expected.label, actual.label)
        assertEquals(expected.icon, actual.icon)
        assertEquals(expected.color, actual.color)
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
}
