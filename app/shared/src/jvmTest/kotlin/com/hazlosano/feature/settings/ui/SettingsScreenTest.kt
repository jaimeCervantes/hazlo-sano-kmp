package com.hazlosano.feature.settings.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hazlosano.core.ui.theme.HazloSanoTheme
import com.hazlosano.domain.settings.ThemePreference
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La pantalla de ajustes: qué opción sale marcada y qué se elige al tocar.
 *
 * Se afirma por `testTag` y no por la redacción: el rótulo de cada opción vive en el catálogo de
 * cadenas y va a cambiar de idioma en el slice 5.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @Test
    fun `the stored preference is the one that shows as chosen`() = runComposeUiTest {
        setContent {
            HazloSanoTheme {
                SettingsContent(
                    theme = ThemePreference.DARK,
                    onChooseTheme = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(SettingsTags.themeOption(ThemePreference.DARK)).assertIsSelected()
    }

    @Test
    fun `an app that was never asked shows following the system as chosen`() = runComposeUiTest {
        setContent {
            HazloSanoTheme {
                SettingsContent(
                    theme = ThemePreference.DEFAULT,
                    onChooseTheme = {},
                    onBack = {},
                )
            }
        }

        onNodeWithTag(SettingsTags.themeOption(ThemePreference.SYSTEM)).assertIsSelected()
    }

    @Test
    fun `each option reports itself when touched`() {
        for (preference in ThemePreference.entries) {
            runComposeUiTest {
                val chosen = mutableListOf<ThemePreference>()
                setContent {
                    HazloSanoTheme {
                        SettingsContent(
                            // Se parte de otra opción para que el toque tenga algo que cambiar.
                            theme = ThemePreference.entries.first { it != preference },
                            onChooseTheme = { chosen += it },
                            onBack = {},
                        )
                    }
                }

                onNodeWithTag(SettingsTags.themeOption(preference)).performClick()

                assertEquals(listOf(preference), chosen)
            }
        }
    }

    /**
     * Se toca la fila entera, no sólo el círculo. Un objetivo de 24 dp es incómodo en un teléfono y
     * no hay nada más en la fila que pueda querer el toque.
     */
    @Test
    fun `the whole row is the target, not just the radio button`() = runComposeUiTest {
        val chosen = mutableListOf<ThemePreference>()
        setContent {
            HazloSanoTheme {
                SettingsContent(
                    theme = ThemePreference.SYSTEM,
                    onChooseTheme = { chosen += it },
                    onBack = {},
                )
            }
        }

        onNodeWithTag(SettingsTags.themeOption(ThemePreference.LIGHT)).performClick()

        assertEquals(listOf(ThemePreference.LIGHT), chosen)
    }
}
