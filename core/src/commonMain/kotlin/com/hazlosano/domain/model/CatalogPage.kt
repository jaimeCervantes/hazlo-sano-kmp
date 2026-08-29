package com.hazlosano.domain.model

/**
 * Lo que el catálogo pudo enseñar, **y de dónde salió**.
 *
 * Los tres casos no son decoración: son los tres finales distintos que el usuario tiene que poder
 * distinguir. Enseñar una lista vacía cuando no hubo red miente igual que enseñar datos viejos sin
 * decir que son viejos.
 *
 * Es un tipo cerrado y sin texto redactado: la UI decide la palabra leyendo el catálogo de cadenas.
 * Un `String` ya escrito aquí sería copia en un módulo que ninguna traducción alcanza.
 */
sealed interface CatalogPage {

    /** Llegó del sitio. Es lo que está publicado ahora mismo. */
    data class Fresh(
        val publications: List<HazloProduct>,
        val hasMore: Boolean = false,
    ) : CatalogPage

    /** No hubo red, pero había algo leído antes. Se enseña diciendo que puede estar desactualizado. */
    data class Cached(
        val publications: List<HazloProduct>,
    ) : CatalogPage

    /**
     * No hubo red y no hay nada guardado: este pilar no se ha leído nunca en este dispositivo.
     *
     * Es distinto de un pilar que de verdad no tiene publicaciones ([Fresh] con lista vacía), y esa
     * diferencia es justo la que evita inventar un catálogo que no existe.
     */
    data object Unavailable : CatalogPage
}
