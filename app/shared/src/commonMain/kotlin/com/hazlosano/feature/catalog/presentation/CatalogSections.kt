package com.hazlosano.feature.catalog.presentation

import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PublicationKind

/**
 * El catálogo de un pilar repartido en las secciones que el tablero pinta.
 *
 * Se calcula una vez al construir el estado y no en cada recomposición, y sobre todo se calcula
 * **fuera del Composable**: qué cuenta como próximo o como cercano es una regla, y una regla dentro
 * de un `@Composable` no se puede probar sin levantar una pantalla.
 */
data class CatalogSections(
    /** Todo lo del pilar, en el orden en que llegó. Es lo que pinta la rejilla de abajo. */
    val all: List<HazloProduct> = emptyList(),
    /** Eventos que todavía no han ocurrido, del más próximo al más lejano. */
    val upcomingEvents: List<HazloProduct> = emptyList(),
    val services: List<HazloProduct> = emptyList(),
    /** Lo que trae distancia, de lo más cercano a lo más lejano. */
    val nearby: List<HazloProduct> = emptyList(),
) {
    val total: Int get() = all.size
    val eventCount: Int get() = all.count { it.kind == PublicationKind.EVENT }
    val serviceCount: Int get() = all.count { it.kind == PublicationKind.SERVICE }
}

/**
 * Reparte las publicaciones de un pilar en secciones.
 *
 * [nowEpochMillis] entra por parámetro en vez de leerse de un reloj aquí dentro: es lo que decide
 * qué evento es "próximo", y una función que consulta la hora por su cuenta no se puede probar sin
 * esperar a que pase el tiempo.
 */
fun catalogSections(
    publications: List<HazloProduct>,
    nowEpochMillis: Long,
): CatalogSections = CatalogSections(
    all = publications,
    upcomingEvents = publications
        .filter { it.kind == PublicationKind.EVENT && it.hasNotEndedBy(nowEpochMillis) }
        // Un evento sin fecha no puede ordenarse entre los que la tienen, y va al final en vez de
        // colarse primero: `sortedBy` pondría el null delante y taparía lo que sí ocurre pronto.
        .sortedWith(compareBy(nullsLast()) { it.startsAtEpochMillis }),
    services = publications.filter { it.kind == PublicationKind.SERVICE },
    nearby = publications
        .filter { it.distanceMeters != null }
        .sortedBy { it.distanceMeters },
)

/**
 * Un evento sigue vigente mientras no haya terminado.
 *
 * Se mira el final y no el principio porque un taller de tres horas al que llegas a la segunda sigue
 * estando en curso; descartarlo en cuanto empieza lo escondería justo cuando es más útil. Sin hora
 * de fin, el propio inicio hace de final, que es lo que hace el sitio.
 */
private fun HazloProduct.hasNotEndedBy(nowEpochMillis: Long): Boolean {
    val endsAt = endsAtEpochMillis ?: startsAtEpochMillis ?: return true
    return endsAt >= nowEpochMillis
}
