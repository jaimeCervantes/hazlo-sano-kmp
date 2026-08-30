package com.hazlosano.domain.usecase

import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.PillarHighlightsRepository

/** Los campeones y retos de un pilar, listos para pintar. */
class GetPillarHighlightsUseCase(
    private val repository: PillarHighlightsRepository,
) {
    suspend operator fun invoke(pillar: PillarType): PillarHighlights =
        repository.getHighlights(pillar)
}
