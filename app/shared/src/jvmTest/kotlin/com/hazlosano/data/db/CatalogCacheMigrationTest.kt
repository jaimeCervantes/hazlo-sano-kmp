package com.hazlosano.data.db

import app.cash.sqldelight.db.SqlDriver
import com.hazlosano.data.product.SqlDelightProductDataSource
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.PublicationKind
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The migration that turns ProductEntity from the catalogue into a cache of it.
 *
 * The point of this test is the seeded rows. They were never read from anywhere — they were 144
 * lines compiled into the app — so carrying them across would make "with no network, show the last
 * thing you read" a lie: a phone that had never reached the site would show the seed and call it
 * the catalogue. Dropping them is what lets the app say "there is no catalogue yet" honestly, and
 * that is the acceptance criterion this migration exists to satisfy.
 */
class CatalogCacheMigrationTest {

    @Test
    fun `the seeded rows do not survive the migration`() = runTest {
        val driver = inMemoryDriver()
        driver.createProductTableAsItWasBeforeTheCatalogue()
        driver.seedAProductTheOldWay()

        driver.migrateFromVersionOne()

        val cache = SqlDelightProductDataSource(HazloSanoDatabase(driver))
        assertEquals(0L, cache.count(), "a hardcoded row survived and would pass as the catalogue")
    }

    @Test
    fun `the table gains what the other three kinds of publication need`() = runTest {
        val driver = inMemoryDriver()
        driver.createProductTableAsItWasBeforeTheCatalogue()

        driver.migrateFromVersionOne()

        val columns = driver.columnsOf("ProductEntity")
        assertTrue(columns.containsAll(setOf("kind", "pillar", "startsAt", "endsAt", "durationMinutes")))
    }

    @Test
    fun `price becomes nullable so an announcement is not forced to be free`() = runTest {
        val driver = inMemoryDriver()
        driver.createProductTableAsItWasBeforeTheCatalogue()
        driver.migrateFromVersionOne()
        val cache = SqlDelightProductDataSource(HazloSanoDatabase(driver))

        cache.replacePillar(
            PillarType.MIND,
            listOf(
                HazloProduct(
                    id = "a1",
                    name = "Aviso de la comunidad",
                    description = "",
                    price = null,
                    kind = PublicationKind.ANNOUNCEMENT,
                    pillar = PillarType.MIND,
                ),
            ),
        )

        assertNull(cache.getByPillar(PillarType.MIND).single().price)
    }

    @Test
    fun `a database that never had the product table gets it`() = runTest {
        val driver = inMemoryDriver()

        driver.migrateFromVersionOne()

        assertTrue(driver.columnsOf("ProductEntity").containsAll(setOf("kind", "pillar")))
    }

    @Test
    fun `a cached pillar reads back with everything it was stored with`() = runTest {
        val driver = inMemoryDriver()
        HazloSanoDatabase.Schema.create(driver)
        val cache = SqlDelightProductDataSource(HazloSanoDatabase(driver))

        cache.replacePillar(
            PillarType.MOVEMENT,
            listOf(
                HazloProduct(
                    id = "e1",
                    name = "Rodada del domingo",
                    description = "Salida en grupo",
                    price = null,
                    kind = PublicationKind.EVENT,
                    pillar = PillarType.MOVEMENT,
                    startsAtEpochMillis = 1_788_620_400_000L,
                    endsAtEpochMillis = 1_788_629_400_000L,
                ),
                HazloProduct(
                    id = "s1",
                    name = "Masaje deportivo",
                    description = "",
                    price = 450.0,
                    kind = PublicationKind.SERVICE,
                    pillar = PillarType.MOVEMENT,
                    durationMinutes = 60,
                ),
            ),
        )

        val rows = cache.getByPillar(PillarType.MOVEMENT).associateBy { it.id }
        assertEquals(PublicationKind.EVENT, rows.getValue("e1").kind)
        assertEquals(1_788_620_400_000L, rows.getValue("e1").startsAtEpochMillis)
        assertEquals(1_788_629_400_000L, rows.getValue("e1").endsAtEpochMillis)
        assertEquals(60, rows.getValue("s1").durationMinutes)
        assertEquals(450.0, rows.getValue("s1").price)
    }

    @Test
    fun `replacing a pillar removes what the site no longer publishes`() = runTest {
        val driver = inMemoryDriver()
        HazloSanoDatabase.Schema.create(driver)
        val cache = SqlDelightProductDataSource(HazloSanoDatabase(driver))
        cache.replacePillar(
            PillarType.NUTRITION,
            listOf(HazloProduct(id = "old", name = "Retirado", pillar = PillarType.NUTRITION)),
        )

        cache.replacePillar(
            PillarType.NUTRITION,
            listOf(HazloProduct(id = "new", name = "Vigente", pillar = PillarType.NUTRITION)),
        )

        assertEquals(listOf("new"), cache.getByPillar(PillarType.NUTRITION).map { it.id })
    }

    @Test
    fun `replacing one pillar leaves the others alone`() = runTest {
        val driver = inMemoryDriver()
        HazloSanoDatabase.Schema.create(driver)
        val cache = SqlDelightProductDataSource(HazloSanoDatabase(driver))
        cache.replacePillar(
            PillarType.SLEEP,
            listOf(HazloProduct(id = "s", name = "De sueño", pillar = PillarType.SLEEP)),
        )

        cache.replacePillar(
            PillarType.NUTRITION,
            listOf(HazloProduct(id = "n", name = "De nutrición", pillar = PillarType.NUTRITION)),
        )

        assertEquals(1, cache.countByPillar(PillarType.SLEEP))
        assertEquals(1, cache.countByPillar(PillarType.NUTRITION))
    }

    @Test
    fun `a pillar never read reports nothing cached`() = runTest {
        val driver = inMemoryDriver()
        HazloSanoDatabase.Schema.create(driver)
        val cache = SqlDelightProductDataSource(HazloSanoDatabase(driver))

        assertEquals(0L, cache.countByPillar(PillarType.MIND))
    }
}

/** ProductEntity as it was while it held the seed: price NOT NULL and no kind. */
private fun SqlDriver.createProductTableAsItWasBeforeTheCatalogue() {
    exec(
        """
        CREATE TABLE SellerEntity (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            category TEXT NOT NULL,
            phone TEXT NOT NULL,
            url TEXT,
            description TEXT,
            logoUrl TEXT,
            hasMembership INTEGER NOT NULL DEFAULT 0,
            hasPaidAds INTEGER NOT NULL DEFAULT 0
        )
        """.trimIndent(),
    )
    exec(
        """
        CREATE TABLE ProductEntity (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            price REAL NOT NULL,
            isAvailable INTEGER NOT NULL DEFAULT 1,
            description TEXT,
            category TEXT NOT NULL,
            subCategory TEXT,
            tags TEXT NOT NULL DEFAULT '[]',
            imageUrl TEXT,
            productUrl TEXT,
            sellerId TEXT,
            FOREIGN KEY (sellerId) REFERENCES SellerEntity(id)
        )
        """.trimIndent(),
    )
}

private fun SqlDriver.seedAProductTheOldWay() {
    exec(
        """
        INSERT INTO ProductEntity(id, name, price, isAvailable, description, category, tags)
        VALUES ('077cacd9', 'Pechuga de pollo a la naranja en bistec', 105.0, 1, 'Sembrado', 'Food', '[]')
        """.trimIndent(),
    )
}
