package com.hazlosano.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * La escala tipográfica del sitio, y el reparto entre sus dos voces.
 *
 * Antes de esto el app declaraba **un** estilo (`bodyLarge`) sobre `FontFamily.Default` y dejaba los
 * catorce restantes en los de Material, así que ningún tamaño de la app tenía que ver con ningún
 * tamaño del sitio. Lo que se fija aquí es que vuelvan a ser la misma escala.
 */
@OptIn(ExperimentalTestApi::class)
class HazloTypographyTest {

    private fun typography(block: (Typography) -> Unit) = runComposeUiTest {
        lateinit var type: Typography
        setContent { type = hazloTypography() }
        block(type)
    }

    @Test
    fun `the scale is the site's scale`() = typography { type ->
        val expected = listOf(
            "displayLarge (--fs-display)" to (type.displayLarge.fontSize to 56.sp),
            "displayMedium (--fs-heading-lg)" to (type.displayMedium.fontSize to 40.sp),
            "headlineMedium (--fs-heading-md)" to (type.headlineMedium.fontSize to 26.sp),
            "headlineSmall (--fs-heading-sm)" to (type.headlineSmall.fontSize to 20.sp),
            "bodyLarge (--fs-body-lg)" to (type.bodyLarge.fontSize to 18.sp),
            "bodyMedium (--fs-body-md)" to (type.bodyMedium.fontSize to 16.sp),
            "bodySmall (--fs-label)" to (type.bodySmall.fontSize to 14.sp),
            "labelMedium (--fs-caption)" to (type.labelMedium.fontSize to 12.sp),
            "labelSmall (--fs-tiny)" to (type.labelSmall.fontSize to 10.sp),
        )

        for ((name, sizes) in expected) {
            assertEquals(sizes.second, sizes.first, "$name no coincide con la escala del sitio.")
        }
    }

    /** Dos voces, y cada una con su trabajo: si acaban siendo la misma, el reparto no existe. */
    @Test
    fun `the display voice is not the interface voice`() = typography { type ->
        assertNotEquals(
            type.displayLarge.fontFamily,
            type.bodyMedium.fontFamily,
            "El titular y el cuerpo usan la misma familia; la marca y la interfaz dejan de sonar distinto.",
        )
    }

    @Test
    fun `no style falls back to the platform font`() = typography { type ->
        val styles = mapOf(
            "displayLarge" to type.displayLarge,
            "displayMedium" to type.displayMedium,
            "displaySmall" to type.displaySmall,
            "headlineLarge" to type.headlineLarge,
            "headlineMedium" to type.headlineMedium,
            "headlineSmall" to type.headlineSmall,
            "titleLarge" to type.titleLarge,
            "titleMedium" to type.titleMedium,
            "titleSmall" to type.titleSmall,
            "bodyLarge" to type.bodyLarge,
            "bodyMedium" to type.bodyMedium,
            "bodySmall" to type.bodySmall,
            "labelLarge" to type.labelLarge,
            "labelMedium" to type.labelMedium,
            "labelSmall" to type.labelSmall,
        )

        for ((name, style) in styles) {
            assertTrue(
                style.fontFamily != null && style.fontFamily != FontFamily.Default,
                "`$name` se quedó en la fuente del sistema en vez de una de las dos de la marca.",
            )
        }
    }
}
