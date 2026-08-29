package com.hazlosano.data.catalog

import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.PublicationKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogMapperTest {

    private fun post(
        id: String = "p1",
        title: String? = "Pechuga de pollo a la naranja en bistec",
        price: Double? = 105.0,
        kind: String? = "producto",
        media: List<CatalogMediaDto> = emptyList(),
        content: String? = "Proteína magra",
        summary: String? = null,
        startsAt: String? = null,
        endsAt: String? = null,
        durationMinutes: Int? = null,
        distanceMeters: Double? = null,
        category: String? = "alimentacion",
        categoryLabel: String? = null,
    ) = CatalogPostDto(
        id = id,
        title = title,
        price = price,
        kind = kind,
        media = media,
        content = content,
        summary = summary,
        startsAt = startsAt,
        endsAt = endsAt,
        durationMinutes = durationMinutes,
        distanceMeters = distanceMeters,
        category = category,
        categoryLabel = categoryLabel,
    )

    @Test
    fun `maps a product with its name and price`() {
        val result = CatalogMapper.toDomain(post(), PillarType.NUTRITION)

        assertEquals("Pechuga de pollo a la naranja en bistec", result.name)
        assertEquals(105.0, result.price)
        assertEquals(PublicationKind.PRODUCT, result.kind)
        assertEquals(PillarType.NUTRITION, result.pillar)
    }

    @Test
    fun `an announcement keeps a null price instead of a zero`() {
        // A 0.0 here would be indistinguishable from something genuinely free, which is a different
        // statement about the publication.
        val result = CatalogMapper.toDomain(post(price = null, kind = "anuncio"), null)

        assertNull(result.price)
        assertEquals(PublicationKind.ANNOUNCEMENT, result.kind)
    }

    @Test
    fun `an event carries its dates as epoch millis`() {
        val result = CatalogMapper.toDomain(
            post(
                kind = "evento",
                price = null,
                startsAt = "2026-09-05T15:00:00Z",
                endsAt = "2026-09-05T17:30:00Z",
            ),
            PillarType.MOVEMENT,
        )

        assertEquals(PublicationKind.EVENT, result.kind)
        assertEquals(1_788_620_400_000L, result.startsAtEpochMillis)
        assertEquals(1_788_629_400_000L, result.endsAtEpochMillis)
    }

    @Test
    fun `a service carries its duration`() {
        val result = CatalogMapper.toDomain(
            post(kind = "servicio", price = 450.0, durationMinutes = 60),
            PillarType.MIND,
        )

        assertEquals(PublicationKind.SERVICE, result.kind)
        assertEquals(60, result.durationMinutes)
    }

    @Test
    fun `an unreadable date is dropped without losing the publication`() {
        val result = CatalogMapper.toDomain(post(kind = "evento", startsAt = "el jueves"), null)

        assertNull(result.startsAtEpochMillis)
        assertEquals("Pechuga de pollo a la naranja en bistec", result.name)
    }

    @Test
    fun `the cover is the first image and a video is skipped`() {
        val result = CatalogMapper.toDomain(
            post(
                media = listOf(
                    CatalogMediaDto(url = "https://x/clip.mp4", type = "video/mp4"),
                    CatalogMediaDto(url = "https://x/foto.webp", type = "image/webp"),
                ),
            ),
            null,
        )

        assertEquals("https://x/foto.webp", result.imageUrl)
    }

    @Test
    fun `no media leaves the image empty rather than null`() {
        // The card checks isBlank() to decide whether to draw its initials placeholder.
        assertTrue(CatalogMapper.toDomain(post(), null).imageUrl.isBlank())
    }

    @Test
    fun `the translated category label wins over the raw key`() {
        val result = CatalogMapper.toDomain(
            post(category = "alimentacion", categoryLabel = "Alimentación"),
            null,
        )

        assertEquals("Alimentación", result.category)
    }

    @Test
    fun `the raw key is kept when the taxonomy has no label`() {
        val result = CatalogMapper.toDomain(post(categoryLabel = null), null)

        assertEquals("alimentacion", result.category)
    }

    @Test
    fun `the summary is preferred over the full content for the card`() {
        val result = CatalogMapper.toDomain(post(summary = "Corto", content = "Muy largo"), null)

        assertEquals("Corto", result.description)
    }

    @Test
    fun `distance travels through when the site measured it`() {
        val result = CatalogMapper.toDomain(post(distanceMeters = 1240.0), null)

        assertEquals(1240.0, result.distanceMeters)
    }

    @Test
    fun `maps a whole page in the order the site sent it`() {
        val response = CatalogResponseDto(
            posts = listOf(post(id = "a"), post(id = "b"), post(id = "c")),
        )

        val result = CatalogMapper.toDomain(response, PillarType.SLEEP)

        assertEquals(listOf("a", "b", "c"), result.map { it.id })
    }
}
