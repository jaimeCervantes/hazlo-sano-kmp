package com.hazlosano.data.catalog

import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.PublicationKind

/**
 * Una fila del buscador, convertida en publicacion.
 *
 * Va aparte de `CatalogMapper` porque la forma de entrada es distinta, no porque la de salida lo
 * sea: las dos producen un [HazloProduct]. Lo que cambia es de donde sale cada campo, y ese
 * "de donde" es justo lo que un mapeador es.
 */
internal object SearchMapper {

    /**
     * El idioma pedido, y si esa traduccion no existe, cualquiera que si.
     *
     * Caer a otra traduccion en vez de dejar el titulo vacio es lo correcto aqui: una publicacion
     * escrita solo en ingles existe y se puede encontrar. Un resultado sin titulo no se puede ni
     * leer ni tocar, que es peor que leerlo en otro idioma.
     */
    fun toDomain(dto: SearchPostDto, locale: String, pillar: PillarType?): HazloProduct {
        val text = dto.translations[locale] ?: dto.translations.values.firstOrNull()

        return HazloProduct(
            id = dto.id,
            name = text?.title.orEmpty(),
            description = text?.content.orEmpty(),
            price = dto.price,
            // El 0 es la portada: el sitio devuelve los archivos ya ordenados.
            imageUrl = dto.media.firstOrNull { it.type?.startsWith("image") == true }?.url.orEmpty(),
            distanceMeters = dto.distanceMeters,
            // Aqui no llega la etiqueta traducida que si trae el feed, solo la clave. Se pinta la
            // clave antes que nada: es fea, pero es verdad, y una categoria en blanco no lo seria.
            category = dto.category.orEmpty(),
            subCategory = dto.subCategory,
            // El buscador no devuelve enlace absoluto. No se compone uno adivinando la ruta del
            // sitio: una URL inventada que lleve a un 404 es peor que no ofrecer enlace.
            productUrl = null,
            sellerId = dto.seller?.id,
            isAvailable = dto.isAvailable,
            kind = PublicationKind.fromKey(dto.kind),
            pillar = pillar,
            startsAtEpochMillis = null,
            endsAtEpochMillis = null,
            durationMinutes = dto.durationMinutes,
        )
    }

    fun toDomain(
        response: SearchResponseDto,
        locale: String,
        pillar: PillarType?,
    ): List<HazloProduct> = response.results.map { toDomain(it, locale, pillar) }
}
