package com.hazlosano.domain.repository

import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.domain.model.PillarType

/**
 * De dónde salen los campeones y los retos de un pilar.
 *
 * Hoy la implementación es contenido de muestra dentro del app; el día que el backend los publique
 * se cambia la implementación y ninguna pantalla se entera. Ése es el motivo de que esta interfaz
 * exista ahora y no cuando llegue el endpoint.
 */
interface PillarHighlightsRepository {

    suspend fun getHighlights(pillar: PillarType): PillarHighlights
}
