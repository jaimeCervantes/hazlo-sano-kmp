package com.hazlosano.data.repository

import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.PillarHighlightsRepository

/**
 * Campeones y retos escritos dentro del app, mientras el backend no los publique.
 *
 * Se llama "Sample" y no "Mock" a propósito: no imita a nadie, es el contenido que la app enseña
 * hoy. El día que exista el endpoint, esta clase se sustituye por la que lo lea.
 */
class SamplePillarHighlightsRepository : PillarHighlightsRepository {

    override suspend fun getHighlights(pillar: PillarType): PillarHighlights =
        SamplePillarHighlights.of(pillar)
}
