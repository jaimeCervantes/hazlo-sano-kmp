package com.hazlosano.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.hazlosano.core.ui.model.of
import com.hazlosano.domain.model.PillarType
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Mide la paleta, no la describe.
 *
 * Es la traducción de `brandPalette.contrast.test.ts` y `pillarPalette.contrast.test.ts` del repo
 * hermano. Existen por lo mismo: la paleta anterior del app tenía tres de los cuatro pilares por
 * debajo de AA —Mente llegaba a 2.14:1— y nada lo dijo durante meses porque nadie estaba midiendo.
 */
class HazloPaletteContrastTest {

    private val themes = listOf("claro" to LightHazloPalette, "oscuro" to DarkHazloPalette)

    @Test
    fun everyPillarFillCarriesWhiteText() {
        for ((themeName, palette) in themes) {
            for (pillar in PillarType.entries) {
                val solid = palette.of(pillar).solid
                assertMeetsAa(
                    foreground = Color.White,
                    background = solid,
                    what = "el blanco sobre el relleno de $pillar en tema $themeName",
                )
            }
        }
    }

    @Test
    fun everyPillarInkReadsOnItsOwnSoftPaper() {
        for ((themeName, palette) in themes) {
            for (pillar in PillarType.entries) {
                val ramp = palette.of(pillar)
                assertMeetsAa(
                    foreground = ramp.ink,
                    background = ramp.soft,
                    what = "la tinta de $pillar sobre su papel tenue en tema $themeName",
                )
            }
        }
    }

    @Test
    fun everyPillarInkReadsOnThePaperOfItsTheme() {
        for ((themeName, palette) in themes) {
            for (pillar in PillarType.entries) {
                assertMeetsAa(
                    foreground = palette.of(pillar).ink,
                    background = palette.surfaceElevation1,
                    what = "la tinta de $pillar sobre la superficie en tema $themeName",
                )
            }
        }
    }

    @Test
    fun thePrimaryButtonCarriesItsOwnLabel() {
        for ((themeName, palette) in themes) {
            assertMeetsAa(
                foreground = palette.buttonPrimaryText,
                background = palette.brandGreen,
                what = "el texto del botón primario en tema $themeName",
            )
        }
    }

    /**
     * La semilla del logo es justamente la que **no** cumple, y por eso no rellena nada. El test la
     * fija: si alguien vuelve a apuntar `brandGreen` a la semilla, esto falla antes que el usuario.
     */
    @Test
    fun theLogoSeedIsNeverWhatTheInterfaceFills() {
        for ((themeName, palette) in themes) {
            assertTrue(
                palette.brandGreen != palette.brandSeed,
                "En tema $themeName el relleno de marca volvió a ser la semilla del logo, que da " +
                    "${contrastRatio(Color.White, palette.brandSeed).format()}:1 con texto blanco.",
            )
        }
    }

    @Test
    fun bodyTextReadsOnAllThreeSurfaces() {
        for ((themeName, palette) in themes) {
            val surfaces = listOf(
                "el papel" to palette.surfaceBackground,
                "la elevación 1" to palette.surfaceElevation1,
                "la elevación 2" to palette.surfaceElevation2,
            )
            for ((surfaceName, surface) in surfaces) {
                assertMeetsAa(
                    foreground = palette.textBase,
                    background = surface,
                    what = "el texto base sobre $surfaceName en tema $themeName",
                )
                assertMeetsAa(
                    foreground = palette.textSupport,
                    background = surface,
                    what = "el texto de apoyo sobre $surfaceName en tema $themeName",
                )
            }
        }
    }

    @Test
    fun everyFeedbackInkReadsOnItsOwnSoftPaper() {
        for ((themeName, palette) in themes) {
            val pairs = listOf(
                Triple("éxito", palette.feedbackSuccessInk, palette.feedbackSuccessSoft),
                Triple("aviso", palette.feedbackWarningInk, palette.feedbackWarningSoft),
                Triple("error", palette.feedbackErrorInk, palette.feedbackErrorSoft),
            )
            for ((name, ink, soft) in pairs) {
                assertMeetsAa(
                    foreground = ink,
                    background = soft,
                    what = "la tinta de $name sobre su papel en tema $themeName",
                )
            }
        }
    }

    private fun assertMeetsAa(foreground: Color, background: Color, what: String) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            ratio >= AA_NORMAL_TEXT,
            "$what da ${ratio.format()}:1, por debajo del mínimo AA de $AA_NORMAL_TEXT:1.",
        )
    }

    private fun Double.format(): String {
        val hundredths = (this * 100).toLong()
        return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
    }
}
