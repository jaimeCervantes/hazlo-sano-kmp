package com.hazlosano.domain.model

/**
 * Lo que un pilar destaca de su comunidad: quién va delante esta semana y qué retos hay abiertos.
 *
 * Las dos listas viajan juntas porque se pintan seguidas y salen de la misma lectura; pedirlas por
 * separado costaría dos esperas para una sola pantalla.
 *
 * Una lista vacía es una respuesta legítima —una semana sin campeones existe— y la pantalla la
 * resuelve no pintando la sección, en lugar de dejar un encabezado sobre una fila vacía.
 */
data class PillarHighlights(
    val champions: List<HazloChampion> = emptyList(),
    val challenges: List<HazloChallenge> = emptyList(),
) {
    val isEmpty: Boolean get() = champions.isEmpty() && challenges.isEmpty()
}
