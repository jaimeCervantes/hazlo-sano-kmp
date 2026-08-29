package com.hazlosano.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PublicationKindTest {

    @Test
    fun `reads the four kinds the site publishes`() {
        assertEquals(PublicationKind.ANNOUNCEMENT, PublicationKind.fromKey("anuncio"))
        assertEquals(PublicationKind.PRODUCT, PublicationKind.fromKey("producto"))
        assertEquals(PublicationKind.EVENT, PublicationKind.fromKey("evento"))
        assertEquals(PublicationKind.SERVICE, PublicationKind.fromKey("servicio"))
    }

    @Test
    fun `an unknown kind falls back instead of losing the publication`() {
        // posts.kind is text with no CHECK in the database, so the site can publish a kind this
        // build has never heard of. Hiding that publication would be worse than drawing it plain.
        assertEquals(PublicationKind.ANNOUNCEMENT, PublicationKind.fromKey("taller"))
        assertEquals(PublicationKind.ANNOUNCEMENT, PublicationKind.fromKey(null))
        assertEquals(PublicationKind.ANNOUNCEMENT, PublicationKind.fromKey(""))
    }

    @Test
    fun `every kind round trips through its wire key`() {
        PublicationKind.entries.forEach { kind ->
            assertEquals(kind, PublicationKind.fromKey(kind.key))
        }
    }
}
