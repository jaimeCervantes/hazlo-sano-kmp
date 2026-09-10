package com.hazlosano.data.catalog

import kotlinx.serialization.Serializable

/**
 * Lo que devuelve el buscador del sitio, que **no es lo que devuelve el feed**.
 *
 * Se comprobó llamando al endpoint antes de escribir esto, y la diferencia importaba: el feed pasa
 * sus publicaciones por un mapeador de tarjetas que aplana el idioma y traduce la categoría; la
 * búsqueda devuelve la fila casi como está en la base.
 *
 * | | feed (`/api/posts`) | búsqueda (`/api/search`) |
 * |---|---|---|
 * | título | `title`, ya en el idioma | **`translations.{locale}.title`** |
 * | contenido | `content` / `summary` | `translations.{locale}.content` |
 * | categoría | `categoryLabel` traducida | sólo `category`, la clave cruda |
 * | enlace | `to` absoluto | **no viene** |
 * | vendedor | trae `id` | trae `handle`, no `id` |
 *
 * Reutilizar `CatalogPostDto` habría compilado y habría dado **tarjetas sin título**: el cliente
 * ignora las claves que no conoce, así que un `title` que no existe no falla, se queda vacío. Es
 * exactamente el fallo silencioso que hizo que esto no se cableara a ciegas.
 *
 * Y una advertencia que la respuesta trae de fábrica: **incluye un objeto `user` con el correo de
 * quien publicó**, sin autenticación de por medio. Aquí no se declara, así que no entra al app ni
 * se guarda. Ver la bitácora.
 */
@Serializable
internal data class SearchResponseDto(
    val results: List<SearchPostDto> = emptyList(),
    /**
     * Cuántos hay en total. **Es lo único con lo que se puede saber si quedan más**: a diferencia
     * del feed, esta respuesta no trae `nextPage`.
     */
    val total: Int = 0,
)

@Serializable
internal data class SearchPostDto(
    val id: String,
    val price: Double? = null,
    val kind: String? = null,
    val origin: String? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val isAvailable: Boolean = true,
    val distanceMeters: Double? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val durationMinutes: Int? = null,
    val media: List<CatalogMediaDto> = emptyList(),
    val seller: CatalogSellerDto? = null,
    /** Por idioma: `es`, `en`. De aquí salen el título y el texto. */
    val translations: Map<String, SearchTranslationDto> = emptyMap(),
)

@Serializable
internal data class SearchTranslationDto(
    val title: String? = null,
    val content: String? = null,
    val slug: String? = null,
)
