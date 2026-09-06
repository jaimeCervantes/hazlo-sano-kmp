package com.hazlosano.domain.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemePreferenceTest {

    @Test
    fun `only the system preference lets the phone decide`() {
        val cases = listOf(
            Triple(ThemePreference.LIGHT, true, false),
            Triple(ThemePreference.LIGHT, false, false),
            Triple(ThemePreference.DARK, true, true),
            Triple(ThemePreference.DARK, false, true),
            Triple(ThemePreference.SYSTEM, true, true),
            Triple(ThemePreference.SYSTEM, false, false),
        )

        for ((preference, systemIsDark, expected) in cases) {
            assertEquals(
                expected,
                preference.resolvesToDark(systemIsDark),
                "$preference con el sistema en ${if (systemIsDark) "oscuro" else "claro"}",
            )
        }
    }

    /**
     * Elegir "claro" con el teléfono en oscuro es el caso que justifica la pantalla entera: si el
     * sistema ganara, el ajuste sería decorativo.
     */
    @Test
    fun `choosing light beats a phone that is in dark mode`() {
        assertEquals(false, ThemePreference.LIGHT.resolvesToDark(systemIsDark = true))
    }

    @Test
    fun `an app that was never asked follows the system`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.DEFAULT)
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStoredValue(null))
    }

    /**
     * Un valor que no se reconoce vuelve a seguir al sistema en vez de tumbar el arranque. Puede
     * llegar de una versión futura que añadió una opción, de una base editada a mano, o de un
     * renombre que alguien haga sin migrar.
     */
    @Test
    fun `a stored value nobody recognises falls back instead of failing`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStoredValue("SEPIA"))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStoredValue(""))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStoredValue("light"))
    }

    @Test
    fun `what gets stored is what comes back`() {
        for (preference in ThemePreference.entries) {
            assertEquals(preference, ThemePreference.fromStoredValue(preference.storedValue))
        }
    }
}
