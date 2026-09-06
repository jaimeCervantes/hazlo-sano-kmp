package com.hazlosano.feature.catalog.presentation

import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PublicationKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogSectionsTest {

    /** 2026-09-05T15:00:00Z. Todas las fechas de abajo se leen respecto a esta. */
    private val now = 1_788_620_400_000L
    private val oneDay = 86_400_000L

    private fun event(id: String, startsAt: Long?, endsAt: Long? = null) = HazloProduct(
        id = id,
        name = "Evento $id",
        kind = PublicationKind.EVENT,
        startsAtEpochMillis = startsAt,
        endsAtEpochMillis = endsAt,
    )

    private fun product(id: String, distance: Double? = null) = HazloProduct(
        id = id,
        name = "Producto $id",
        price = 45.0,
        kind = PublicationKind.PRODUCT,
        distanceMeters = distance,
    )

    private fun service(id: String) = HazloProduct(
        id = id,
        name = "Servicio $id",
        price = 450.0,
        kind = PublicationKind.SERVICE,
        durationMinutes = 60,
    )

    @Test
    fun `counts what the pillar holds`() {
        val sections = catalogSections(
            listOf(product("p1"), product("p2"), event("e1", now + oneDay), service("s1")),
            now,
        )

        assertEquals(4, sections.total)
        assertEquals(1, sections.eventCount)
        assertEquals(1, sections.serviceCount)
    }

    @Test
    fun `an event that already ended is not announced as upcoming`() {
        val sections = catalogSections(listOf(event("past", now - oneDay)), now)

        assertTrue(sections.upcomingEvents.isEmpty())
    }

    @Test
    fun `an event that already ended still exists in the grid`() {
        // Dejar de anunciarlo no es esconderlo: la rejilla sigue siendo todo lo del pilar.
        val sections = catalogSections(listOf(event("past", now - oneDay)), now)

        assertEquals(listOf("past"), sections.all.map { it.id })
    }

    @Test
    fun `an event under way is still upcoming`() {
        // Un taller de tres horas al que llegas a la segunda sigue estando en curso.
        val underWay = event("live", startsAt = now - 3_600_000L, endsAt = now + 7_200_000L)

        val sections = catalogSections(listOf(underWay), now)

        assertEquals(listOf("live"), sections.upcomingEvents.map { it.id })
    }

    @Test
    fun `without an end date the start is what decides`() {
        val justStarted = event("just", startsAt = now)
        val startedYesterday = event("old", startsAt = now - oneDay)

        val sections = catalogSections(listOf(justStarted, startedYesterday), now)

        assertEquals(listOf("just"), sections.upcomingEvents.map { it.id })
    }

    @Test
    fun `upcoming events come out in the order they happen`() {
        val sections = catalogSections(
            listOf(
                event("third", now + 7 * oneDay),
                event("first", now + oneDay),
                event("second", now + 3 * oneDay),
            ),
            now,
        )

        assertEquals(listOf("first", "second", "third"), sections.upcomingEvents.map { it.id })
    }

    @Test
    fun `an event with no date goes last instead of jumping the queue`() {
        val sections = catalogSections(
            listOf(event("undated", null), event("dated", now + oneDay)),
            now,
        )

        assertEquals(listOf("dated", "undated"), sections.upcomingEvents.map { it.id })
    }





    @Test
    fun `the grid keeps the order the site sent`() {
        // El sitio ya ordenó por cercanía o por fecha; reordenar aquí tiraría lo único que el app
        // no sabe reproducir sin red.
        val sections = catalogSections(
            listOf(product("c"), product("a"), product("b")),
            now,
        )

        assertEquals(listOf("c", "a", "b"), sections.all.map { it.id })
    }

    @Test
    fun `an empty pillar produces empty sections and not nulls`() {
        val sections = catalogSections(emptyList(), now)

        assertEquals(0, sections.total)
        assertTrue(sections.upcomingEvents.isEmpty())
    }
}
