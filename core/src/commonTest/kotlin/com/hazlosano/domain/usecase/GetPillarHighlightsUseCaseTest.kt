package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.HazloChallenge
import com.hazlosano.domain.model.HazloChampion
import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.PillarHighlightsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetPillarHighlightsUseCaseTest {

    private class RecordingRepository(
        private val answer: PillarHighlights,
    ) : PillarHighlightsRepository {
        var asked: PillarType? = null

        override suspend fun getHighlights(pillar: PillarType): PillarHighlights {
            asked = pillar
            return answer
        }
    }

    private val movement = PillarHighlights(
        champions = listOf(
            HazloChampion(name = "Mateo R.", title = "Top Trekker", stat = "42.5 km", imageUrl = ""),
        ),
        challenges = listOf(
            HazloChallenge(
                title = "Montañero 50k",
                description = "Acumula 50 km de elevación en los próximos 30 días.",
                progressText = "12 km / 50 km",
                progress = 0.24f,
                imageUrl = "",
            ),
        ),
    )

    @Test
    fun `it asks for the pillar it was given`() = runTest {
        val repository = RecordingRepository(movement)

        GetPillarHighlightsUseCase(repository)(PillarType.MOVEMENT)

        assertEquals(PillarType.MOVEMENT, repository.asked)
    }

    @Test
    fun `it hands back untouched what the repository says`() = runTest {
        val highlights = GetPillarHighlightsUseCase(RecordingRepository(movement))(PillarType.MOVEMENT)

        assertEquals("Mateo R.", highlights.champions.single().name)
        assertEquals("Montañero 50k", highlights.challenges.single().title)
    }

    @Test
    fun `a pillar with nothing to highlight is empty and not an error`() = runTest {
        val highlights = GetPillarHighlightsUseCase(RecordingRepository(PillarHighlights()))(
            PillarType.MIND,
        )

        assertTrue(highlights.isEmpty)
    }
}
