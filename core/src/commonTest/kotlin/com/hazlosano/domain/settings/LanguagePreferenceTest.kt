package com.hazlosano.domain.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguagePreferenceTest {

    @Test
    fun `an app that was never asked follows the system`() {
        assertEquals(LanguagePreference.SYSTEM, LanguagePreference.DEFAULT)
        assertEquals(LanguagePreference.SYSTEM, LanguagePreference.fromStoredValue(null))
    }

    /** Seguir al sistema es no pedir ningún idioma, y por eso su etiqueta es nula. */
    @Test
    fun `following the system asks for no language of its own`() {
        assertNull(LanguagePreference.SYSTEM.languageTag)
    }

    @Test
    fun `each language carries the tag that names its catalogue folder`() {
        assertEquals("es", LanguagePreference.SPANISH.languageTag)
        assertEquals("en", LanguagePreference.ENGLISH.languageTag)
    }

    /**
     * Puede llegar de una versión futura que añadió un idioma, o de una base editada a mano. Un
     * ajuste no puede tumbar el arranque.
     */
    @Test
    fun `a stored value this version does not know falls back to the system`() {
        assertEquals(LanguagePreference.SYSTEM, LanguagePreference.fromStoredValue("PORTUGUESE"))
        assertEquals(LanguagePreference.SYSTEM, LanguagePreference.fromStoredValue("es"))
        assertEquals(LanguagePreference.SYSTEM, LanguagePreference.fromStoredValue(""))
    }

    @Test
    fun `what gets stored is what comes back`() {
        for (preference in LanguagePreference.entries) {
            assertEquals(preference, LanguagePreference.fromStoredValue(preference.storedValue))
        }
    }
}
