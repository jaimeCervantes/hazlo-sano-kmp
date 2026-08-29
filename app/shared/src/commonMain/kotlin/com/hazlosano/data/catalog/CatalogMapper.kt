package com.hazlosano.data.catalog

import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.PublicationKind
import kotlinx.datetime.Instant

/**
 * De la forma del sitio a la del dominio.
 *
 * Junto a `CatalogDto`, el único punto de acoplamiento con el mapper del web. Está aparte del
 * cliente HTTP para poder probarlo sin red: lo que decide si el catálogo se lee bien es esto, no
 * el transporte.
 */
internal object CatalogMapper {

    fun toDomain(dto: CatalogPostDto, pillar: PillarType?): HazloProduct = HazloProduct(
        id = dto.id,
        name = dto.title.orEmpty(),
        description = dto.summary ?: dto.content.orEmpty(),
        price = dto.price,
        // El 0 es la portada: el sitio devuelve los archivos ya ordenados por `sort_order`.
        imageUrl = dto.media.firstOrNull { it.isImage }?.url.orEmpty(),
        distanceMeters = dto.distanceMeters,
        // La etiqueta traducida gana a la clave: es lo que se pinta. La clave queda de respaldo
        // para que una taxonomía incompleta no deje la tarjeta sin categoría ninguna.
        category = dto.categoryLabel ?: dto.category.orEmpty(),
        subCategory = dto.subCategory,
        productUrl = dto.to,
        sellerId = dto.seller?.id,
        isAvailable = dto.isAvailable,
        kind = PublicationKind.fromKey(dto.kind),
        pillar = pillar,
        startsAtEpochMillis = dto.startsAt.toEpochMillisOrNull(),
        endsAtEpochMillis = dto.endsAt.toEpochMillisOrNull(),
        durationMinutes = dto.durationMinutes,
    )

    fun toDomain(response: CatalogResponseDto, pillar: PillarType?): List<HazloProduct> =
        response.posts.map { toDomain(it, pillar) }

    /**
     * Un vídeo no sirve de portada en una tarjeta, y el tipo llega como texto libre desde la base.
     * Se acepta lo que se declare imagen y se descarta el resto en vez de pintar un `<video>` roto.
     */
    private val CatalogMediaDto.isImage: Boolean
        get() = !url.isNullOrBlank() && (type == null || type.startsWith("image"))

    /**
     * Una fecha ilegible se descarta sin arrastrar la publicación entera.
     *
     * El sitio serializa `Date` a ISO 8601, pero `startsAt` está tipada como `Date | string | null`
     * y quien la escriba a mano puede mandar cualquier cosa. Perder la fecha de un evento degrada
     * la tarjeta; perder el evento lo esconde.
     */
    private fun String?.toEpochMillisOrNull(): Long? {
        if (this.isNullOrBlank()) return null
        return runCatching { Instant.parse(this).toEpochMilliseconds() }.getOrNull()
    }
}
