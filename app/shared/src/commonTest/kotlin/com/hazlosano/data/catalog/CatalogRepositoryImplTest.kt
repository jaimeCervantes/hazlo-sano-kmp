package com.hazlosano.data.catalog

import com.hazlosano.data.product.ProductDataSource
import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.HazloSeller
import com.hazlosano.domain.model.PillarType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CatalogRepositoryImplTest {

    /** Caché en memoria con la misma semántica que la de SQLDelight. */
    private class FakeCache : ProductDataSource {
        val rows = mutableListOf<HazloProduct>()
        var replaceCalls = 0

        override suspend fun searchProducts(query: String, limit: Int, offset: Int) = rows
        override suspend fun getByCategory(category: String, limit: Int, offset: Int) = rows
        override suspend fun getAll(limit: Int, offset: Int) = rows
        override suspend fun getById(id: String) = rows.firstOrNull { it.id == id }

        override suspend fun getByPillar(pillar: PillarType, limit: Int, offset: Int): List<HazloProduct> =
            rows.filter { it.pillar == pillar }.drop(offset).take(limit)

        override suspend fun countByPillar(pillar: PillarType): Long =
            rows.count { it.pillar == pillar }.toLong()

        override suspend fun replacePillar(pillar: PillarType, publications: List<HazloProduct>) {
            replaceCalls++
            rows.removeAll { it.pillar == pillar }
            rows.addAll(publications)
        }

        override suspend fun saveProducts(products: List<HazloProduct>) { rows.addAll(products) }
        override suspend fun saveSellers(sellers: List<HazloSeller>) = Unit
        override suspend fun count(): Long = rows.size.toLong()
        override suspend fun deleteAll() { rows.clear() }
    }

    private fun onlineApi(body: String): CatalogApi {
        val engine = MockEngine {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return CatalogApi(
            HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
            baseUrl = "https://example.test",
        )
    }

    private fun offlineApi(): CatalogApi {
        val engine = MockEngine { throw kotlin.IllegalStateException("no network") }
        return CatalogApi(
            HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
            baseUrl = "https://example.test",
        )
    }

    private val onePost = """{"posts":[{"id":"p1","title":"Suero natural","price":45.0,"kind":"producto"}],"nextPage":null}"""

    @Test
    fun `with network it shows what the site publishes`() = runTest {
        val repository = CatalogRepositoryImpl(onlineApi(onePost), FakeCache())

        val page = repository.getPillarCatalog(PillarType.NUTRITION)

        val fresh = assertIs<CatalogPage.Fresh>(page)
        assertEquals("Suero natural", fresh.publications.single().name)
        assertEquals(45.0, fresh.publications.single().price)
    }

    @Test
    fun `a fresh read replaces what the pillar had cached`() = runTest {
        val cache = FakeCache()
        cache.rows += HazloProduct(id = "old", name = "Retirado", pillar = PillarType.NUTRITION)

        CatalogRepositoryImpl(onlineApi(onePost), cache).getPillarCatalog(PillarType.NUTRITION)

        // Sin el borrado, una publicación retirada del sitio viviría en el dispositivo para siempre.
        assertEquals(listOf("p1"), cache.rows.map { it.id })
    }

    @Test
    fun `only the first page replaces the cache`() = runTest {
        val cache = FakeCache()

        CatalogRepositoryImpl(onlineApi(onePost), cache).getPillarCatalog(PillarType.NUTRITION, page = 3)

        // Guardar la página 3 borrando las anteriores dejaría un trozo suelto del catálogo.
        assertEquals(0, cache.replaceCalls)
    }

    @Test
    fun `without network it shows the last thing it read`() = runTest {
        val cache = FakeCache()
        cache.rows += HazloProduct(id = "p9", name = "Leído ayer", pillar = PillarType.MOVEMENT)

        val page = CatalogRepositoryImpl(offlineApi(), cache).getPillarCatalog(PillarType.MOVEMENT)

        val cached = assertIs<CatalogPage.Cached>(page)
        assertEquals("Leído ayer", cached.publications.single().name)
    }

    @Test
    fun `without network and having never read it says so`() = runTest {
        val page = CatalogRepositoryImpl(offlineApi(), FakeCache()).getPillarCatalog(PillarType.MIND)

        // Ni una lista vacía ni el seed: el usuario tiene que poder distinguir "no hay nada" de
        // "no lo he podido leer".
        assertEquals(CatalogPage.Unavailable, page)
    }

    @Test
    fun `a pillar cached under another pillar does not count as read`() = runTest {
        val cache = FakeCache()
        cache.rows += HazloProduct(id = "p9", name = "De nutrición", pillar = PillarType.NUTRITION)

        val page = CatalogRepositoryImpl(offlineApi(), cache).getPillarCatalog(PillarType.MIND)

        assertEquals(CatalogPage.Unavailable, page)
    }

    @Test
    fun `an empty pillar with network is empty and not unavailable`() = runTest {
        val repository = CatalogRepositoryImpl(onlineApi("""{"posts":[]}"""), FakeCache())

        val page = repository.getPillarCatalog(PillarType.SLEEP)

        val fresh = assertIs<CatalogPage.Fresh>(page)
        assertTrue(fresh.publications.isEmpty())
    }

    @Test
    fun `hasMore follows the next page the site announced`() = runTest {
        val withNext = """{"posts":[{"id":"p1","title":"Uno"}],"nextPage":2}"""

        val page = CatalogRepositoryImpl(onlineApi(withNext), FakeCache())
            .getPillarCatalog(PillarType.NUTRITION)

        assertTrue(assertIs<CatalogPage.Fresh>(page).hasMore)
    }
}
