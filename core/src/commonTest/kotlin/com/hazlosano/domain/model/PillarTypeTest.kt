package com.hazlosano.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PillarTypeTest {

    @Test
    fun `the keys are the ones the site accepts`() {
        // These four strings are a contract with the web's PUBLICATION_PILLARS. Changing one here
        // silently empties a whole tab, so they are asserted literally rather than derived.
        assertEquals("sleep", PillarType.SLEEP.key)
        assertEquals("nutrition", PillarType.NUTRITION.key)
        assertEquals("movement", PillarType.MOVEMENT.key)
        assertEquals("mindSpirit", PillarType.MIND.key)
    }

    @Test
    fun `every pillar round trips through its key`() {
        PillarType.entries.forEach { pillar ->
            assertEquals(pillar, PillarType.fromKey(pillar.key))
        }
    }

    @Test
    fun `an unknown key is no pillar rather than a wrong one`() {
        assertNull(PillarType.fromKey("mind"))
        assertNull(PillarType.fromKey(null))
    }
}
