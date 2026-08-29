package com.hazlosano.data.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * La respuesta de `GET /api/posts/page/{n}/pageSize/{m}` tal cual la manda el sitio.
 *
 * **Este archivo y `CatalogMapper` son los dos únicos sitios del app que conocen esa forma.** El
 * endpoint está modelado para el scroll infinito del web y no es un contrato público: si el sitio
 * cambia su mapper, lo que se rompe se rompe aquí y no repartido por la app.
 *
 * `ignoreUnknownKeys` está activado en el cliente, así que el sitio puede añadir campos sin
 * romper esta versión del app. Lo que sí rompe es quitar o renombrar los que se leen abajo.
 */
@Serializable
internal data class CatalogResponseDto(
    val posts: List<CatalogPostDto> = emptyList(),
    val nextPage: Int? = null,
    val prevPage: Int? = null,
    val total: Int? = null,
)

@Serializable
internal data class CatalogPostDto(
    val id: String,
    val title: String? = null,
    val slug: String? = null,
    val content: String? = null,
    val summary: String? = null,
    /** Nulo en un anuncio y en un evento gratis. No se sustituye por cero. */
    val price: Double? = null,
    val kind: String? = null,
    val origin: String? = null,
    val category: String? = null,
    @SerialName("subCategory") val subCategory: String? = null,
    /** La etiqueta ya resuelta en el idioma pedido. El sitio la calcula; el app no la deriva. */
    val categoryLabel: String? = null,
    val isAvailable: Boolean = true,
    /** Solo llega cuando la petición mandó ubicación. */
    val distanceMeters: Double? = null,
    /** ISO 8601. Solo un evento las trae. */
    val startsAt: String? = null,
    val endsAt: String? = null,
    /** Solo un servicio la trae. */
    val durationMinutes: Int? = null,
    val media: List<CatalogMediaDto> = emptyList(),
    val seller: CatalogSellerDto? = null,
    /** URL absoluta a la publicación en el sitio. */
    val to: String? = null,
)

@Serializable
internal data class CatalogMediaDto(
    val url: String? = null,
    val type: String? = null,
    val alt: String? = null,
)

@Serializable
internal data class CatalogSellerDto(
    val id: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val logoUrl: String? = null,
)
