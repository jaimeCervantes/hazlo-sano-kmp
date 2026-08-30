package com.hazlosano.data.repository

import com.hazlosano.domain.model.PillarType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La corrida de escritorio del contenido de muestra: cada pilar tiene el suyo y ninguno se queda sin
 * nada. Es lo que impide que un pilar acabe enseñando los campeones de otro tras un copiar y pegar.
 */
class SamplePillarHighlightsRepositoryTest {

    private val repository = SamplePillarHighlightsRepository()

    @Test
    fun `each pillar leads with its own champion and its own challenge`() = runTest {
        val expected = mapOf(
            PillarType.MOVEMENT to ("Mateo R." to "Montañero 50k"),
            PillarType.NUTRITION to ("Mateo R." to "Sin Azúcar Añadida"),
            PillarType.MIND to ("Ana V." to "Respira 4-7-8"),
            PillarType.SLEEP to ("Valeria N." to "Apagar pantallas"),
        )

        expected.forEach { (pillar, first) ->
            val highlights = repository.getHighlights(pillar)
            val (champion, challenge) = first

            assertEquals(champion, highlights.champions.first().name, "campeón de $pillar")
            assertEquals(challenge, highlights.challenges.first().title, "reto de $pillar")
        }
    }

    @Test
    fun `the stat each champion shows is the one the pillar measures`() = runTest {
        assertEquals("42.5 km", repository.getHighlights(PillarType.MOVEMENT).champions.first().stat)
        assertEquals("12 recetas", repository.getHighlights(PillarType.NUTRITION).champions.first().stat)
        assertEquals("7 pausas", repository.getHighlights(PillarType.MIND).champions.first().stat)
        assertEquals("7 noches", repository.getHighlights(PillarType.SLEEP).champions.first().stat)
    }

    @Test
    fun `no pillar is left without highlights`() = runTest {
        PillarType.entries.forEach { pillar ->
            val highlights = repository.getHighlights(pillar)

            assertTrue(highlights.champions.isNotEmpty(), "campeones de $pillar")
            assertTrue(highlights.challenges.isNotEmpty(), "retos de $pillar")
        }
    }

    @Test
    fun `the avatar of a champion is drawn in the accent of its own pillar`() = runTest {
        val sleep = repository.getHighlights(PillarType.SLEEP).champions.first()
        val mind = repository.getHighlights(PillarType.MIND).champions.first()

        assertTrue(sleep.imageUrl.endsWith("backgroundColor=8b5cf6"), sleep.imageUrl)
        assertTrue(mind.imageUrl.endsWith("backgroundColor=38bdf8"), mind.imageUrl)
    }
}
