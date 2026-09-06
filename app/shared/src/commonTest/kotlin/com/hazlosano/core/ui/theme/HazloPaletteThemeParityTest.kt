package com.hazlosano.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.hazlosano.core.ui.model.of
import com.hazlosano.domain.model.PillarType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * El equivalente en Compose de `darkThemeParity.test.ts`, con un objetivo distinto porque el riesgo
 * es distinto.
 *
 * En el sitio, el tema oscuro se declara **dos veces** —una media query y un atributo— porque CSS no
 * deja compartir un bloque, y aquella prueba compara las dos copias declaración por declaración.
 * Aquí no hay dos copias: hay un `data class` y dos instancias, así que el compilador ya obliga a
 * que ningún token se quede sin valor en un tema. Lo que el compilador **no** puede ver es un token
 * que se copió del claro por descuido y deja el oscuro ilegible. Eso es lo que se mide aquí.
 */
class HazloPaletteThemeParityTest {

    @Test
    fun theSurfacesAndInksActuallyChangeBetweenThemes() {
        val mustDiffer = listOf(
            "surfaceBackground" to (LightHazloPalette.surfaceBackground to DarkHazloPalette.surfaceBackground),
            "surfaceElevation1" to (LightHazloPalette.surfaceElevation1 to DarkHazloPalette.surfaceElevation1),
            "surfaceElevation2" to (LightHazloPalette.surfaceElevation2 to DarkHazloPalette.surfaceElevation2),
            "textBase" to (LightHazloPalette.textBase to DarkHazloPalette.textBase),
            "textSupport" to (LightHazloPalette.textSupport to DarkHazloPalette.textSupport),
            "textMuted" to (LightHazloPalette.textMuted to DarkHazloPalette.textMuted),
            "border" to (LightHazloPalette.border to DarkHazloPalette.border),
            "borderField" to (LightHazloPalette.borderField to DarkHazloPalette.borderField),
            "separator" to (LightHazloPalette.separator to DarkHazloPalette.separator),
            "brandGreen" to (LightHazloPalette.brandGreen to DarkHazloPalette.brandGreen),
            "buttonPrimaryText" to (LightHazloPalette.buttonPrimaryText to DarkHazloPalette.buttonPrimaryText),
        )

        for ((name, pair) in mustDiffer) {
            assertNotEquals(
                pair.first,
                pair.second,
                "El token `$name` vale lo mismo en claro y en oscuro. Sobre el papel contrario no " +
                    "se lee: si de verdad tiene que ser el mismo, dilo aquí con su motivo.",
            )
        }
    }

    @Test
    fun everyPillarSoftAndInkFlipWithTheTheme() {
        for (pillar in PillarType.entries) {
            val light = LightHazloPalette.of(pillar)
            val dark = DarkHazloPalette.of(pillar)
            assertNotEquals(light.soft, dark.soft, "El papel tenue de $pillar no cambia con el tema.")
            assertNotEquals(light.ink, dark.ink, "La tinta de $pillar no cambia con el tema.")
        }
    }

    /**
     * `solid` es el que **no** debe moverse: lleva texto blanco encima en los dos temas y ya cumple
     * en los dos, así que cambiarlo sólo rompería el reconocimiento del pilar entre plataformas.
     */
    @Test
    fun everyPillarFillIsTheSameInBothThemes() {
        for (pillar in PillarType.entries) {
            assertEquals(
                LightHazloPalette.of(pillar).solid,
                DarkHazloPalette.of(pillar).solid,
                "El relleno de $pillar cambió con el tema; debe ser el mismo en los dos.",
            )
        }
    }

    /**
     * La paleta jubilada, fijada por su hex.
     *
     * No es nostalgia: son los cuatro valores concretos con los que el app llevaba meses, y tres de
     * ellos no llegaban a AA. Si una fusión los devuelve, esto falla en vez de que lo descubra
     * alguien mirando la pantalla al sol.
     */
    @Test
    fun theRetiredPaletteNeverComesBack() {
        val retired = mapOf(
            PillarType.SLEEP to Color(0xFF8B5CF6),
            PillarType.NUTRITION to Color(0xFFF0380E),
            PillarType.MOVEMENT to Color(0xFF538F39),
            PillarType.MIND to Color(0xFF38BDF8),
        )

        for ((pillar, oldValue) in retired) {
            val ramp = LightHazloPalette.of(pillar)
            assertTrue(
                oldValue !in listOf(ramp.solid, ramp.ink),
                "$pillar volvió al color jubilado, que da " +
                    "${contrastRatio(Color.White, oldValue)} con texto blanco encima.",
            )
        }
    }
}
