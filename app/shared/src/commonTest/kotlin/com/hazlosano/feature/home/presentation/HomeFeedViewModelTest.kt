package com.hazlosano.feature.home.presentation

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.repository.CatalogRepository
import com.hazlosano.domain.usecase.GetCatalogUseCase
import com.hazlosano.domain.usecase.SearchCatalogUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * El feed del Inicio: acumular páginas sin mezclar filtros y sin pedir dos veces lo mismo.
 *
 * Spec: `features/publicaciones_en_el_home.feature`, slice 1.
 */
class HomeFeedViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun publication(id: String) = HazloProduct(
        id = id,
        name = "Publicación $id",
        description = "",
        price = null,
        imageUrl = "",
        category = "",
    )

    /**
     * Un catálogo de mentira que recuerda **con qué se le preguntó**.
     *
     * Guardar las peticiones es lo que permite afirmar lo que de verdad importa aquí: que el filtro
     * viaja, que la página crece, y que no se pide dos veces la misma.
     */
    private class FakeCatalog(
        private val pagesPerPillar: Int = 2,
        private val pageSize: Int = 20,
    ) : CatalogRepository {
        data class Ask(val pillar: PillarType?, val page: Int, val query: String = "")

        val asks = mutableListOf<Ask>()
        var failFrom: Int? = null
        var cachedFrom: Int? = null

        override suspend fun searchCatalog(
            query: String,
            pillar: PillarType?,
            page: Int,
            pageSize: Int,
            location: VisitorLocation?,
        ): CatalogPage {
            asks += Ask(pillar, page, query)
            return CatalogPage.Fresh(
                publications = List(this.pageSize) { i -> product(pillar, page, i) },
                hasMore = page < pagesPerPillar,
            )
        }

        override suspend fun getCatalog(
            pillar: PillarType?,
            page: Int,
            pageSize: Int,
            location: VisitorLocation?,
        ): CatalogPage {
            asks += Ask(pillar, page)
            failFrom?.let { if (page >= it) return CatalogPage.Unavailable }
            cachedFrom?.let {
                if (page >= it) {
                    return CatalogPage.Cached(List(this.pageSize) { i -> product(pillar, page, i) })
                }
            }
            return CatalogPage.Fresh(
                publications = List(this.pageSize) { i -> product(pillar, page, i) },
                hasMore = page < pagesPerPillar,
            )
        }

        private fun product(pillar: PillarType?, page: Int, index: Int) = HazloProduct(
            id = "${pillar?.key ?: "all"}-$page-$index",
            name = "p",
            description = "",
            price = null,
            imageUrl = "",
            category = "",
            pillar = pillar,
        )
    }

    private fun feed(catalog: FakeCatalog) = HomeFeedViewModel(
        getCatalog = GetCatalogUseCase(catalog),
        searchCatalog = SearchCatalogUseCase(catalog),
        pageSize = 20,
        typingPause = TYPING_PAUSE,
    )

    private companion object {
        val TYPING_PAUSE = 350.milliseconds
    }

    @Test
    fun `it opens with no filter and asks for the first page`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(FakeCatalog.Ask(pillar = null, page = 1)), catalog.asks)
        assertEquals(null, model.state.value.filter, "no abre con ningún pilar puesto")
        assertEquals(20, model.state.value.publications.size)
    }

    @Test
    fun `reaching the end asks for the next page and keeps what was there`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        model.loadMore()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(1, 2), catalog.asks.map { it.page })
        assertEquals(40, model.state.value.publications.size, "la página 2 sustituyó a la 1")
    }

    /**
     * La guarda que la pantalla obliga a tener.
     *
     * El feed pide más **al pintar la última publicación**, y ese final se alcanza varias veces
     * mientras una petición está en vuelo. Sin esto, un scroll decidido dispara media docena de
     * peticiones y las publicaciones llegan repetidas.
     */
    @Test
    fun `it does not ask for two pages at once`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        model.loadMore()
        model.loadMore()
        model.loadMore()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(1, 2), catalog.asks.map { it.page })
    }

    @Test
    fun `nor for one past the last`() = runTest(dispatcher) {
        val catalog = FakeCatalog(pagesPerPillar = 2)
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        model.loadMore()
        testScheduler.advanceUntilIdle()
        assertFalse(model.state.value.hasMore)

        model.loadMore()
        testScheduler.advanceUntilIdle()

        assertEquals(2, catalog.asks.size, "pidió una página que no existe")
    }

    @Test
    fun `the filter travels with every page`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        model.select(PillarType.MOVEMENT)
        testScheduler.advanceUntilIdle()
        model.loadMore()
        testScheduler.advanceUntilIdle()

        val filtered = catalog.asks.filter { it.pillar == PillarType.MOVEMENT }
        assertEquals(listOf(1, 2), filtered.map { it.page })
    }

    /** Cambiar de pilar empieza de nuevo: dos filtros en una lista no corresponden al chip. */
    @Test
    fun `changing the filter starts over`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        model.loadMore()
        testScheduler.advanceUntilIdle()
        assertEquals(40, model.state.value.publications.size)

        model.select(PillarType.SLEEP)
        testScheduler.advanceUntilIdle()

        assertEquals(20, model.state.value.publications.size, "quedaron mezclados dos filtros")
        assertTrue(model.state.value.publications.all { it.pillar == PillarType.SLEEP })
    }

    /** Elegir el que ya está puesto no tira lo cargado para volver a pedirlo igual. */
    @Test
    fun `choosing the filter that is already on does nothing`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        model.loadMore()
        testScheduler.advanceUntilIdle()
        val asksBefore = catalog.asks.size

        model.select(null)
        testScheduler.advanceUntilIdle()

        assertEquals(asksBefore, catalog.asks.size)
        assertEquals(40, model.state.value.publications.size)
    }

    /**
     * Perder la red no borra lo leído.
     *
     * Quien lleva dos páginas bajadas y entra en un túnel se queda con sus dos páginas y un aviso,
     * no con una pantalla en blanco — que sería castigarle por haber llegado lejos.
     */
    @Test
    fun `a failed page keeps what was already read`() = runTest(dispatcher) {
        val catalog = FakeCatalog(pagesPerPillar = 5)
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        catalog.failFrom = 2

        model.loadMore()
        testScheduler.advanceUntilIdle()

        assertEquals(20, model.state.value.publications.size, "vació lo que ya se había leído")
        assertTrue(model.state.value.failed)
    }

    @Test
    fun `and it can be retried`() = runTest(dispatcher) {
        val catalog = FakeCatalog(pagesPerPillar = 5)
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        catalog.failFrom = 2
        model.loadMore()
        testScheduler.advanceUntilIdle()

        catalog.failFrom = null
        model.retry()
        testScheduler.advanceUntilIdle()

        assertEquals(40, model.state.value.publications.size)
        assertFalse(model.state.value.failed)
    }

    /** Lo guardado se enseña diciendo que puede estar viejo, como ya hace el catálogo de un pilar. */
    @Test
    fun `reading from the cache says so`() = runTest(dispatcher) {
        val catalog = FakeCatalog(pagesPerPillar = 5)
        catalog.cachedFrom = 1
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        assertTrue(model.state.value.stale)
        assertEquals(20, model.state.value.publications.size)
    }

    // ─────────── La búsqueda, que sí le pregunta al sitio ───────────

    /**
     * Escribir no dispara una petición por tecla.
     *
     * Sin la espera, «miel» son cuatro búsquedas de las que tres sobran — y llegan desordenadas, así
     * que la de «mie» puede pisar a la de «miel». Cancelar la anterior es lo que lo impide.
     */
    @Test
    fun `typing a word is one search and not one per letter`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        val before = catalog.asks.size

        model.search("m")
        model.search("mi")
        model.search("mie")
        model.search("miel")
        testScheduler.advanceUntilIdle()

        val searches = catalog.asks.drop(before)
        assertEquals(1, searches.size, "pidió una búsqueda por tecla")
        assertEquals("miel", searches.single().query)
    }

    /** Lo escrito aparece ya, aunque la petición espere: el campo no puede ir con retraso. */
    @Test
    fun `what is typed shows up before the request goes out`() = runTest(dispatcher) {
        val model = feed(FakeCatalog())
        testScheduler.advanceUntilIdle()

        model.search("miel")

        assertEquals("miel", model.state.value.query)
    }

    @Test
    fun `a search asks the site and replaces the feed`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        model.loadMore()
        testScheduler.advanceUntilIdle()
        assertEquals(40, model.state.value.publications.size)

        model.search("miel")
        testScheduler.advanceUntilIdle()

        assertEquals(20, model.state.value.publications.size, "quedaron resultados del feed mezclados")
        assertEquals("miel", catalog.asks.last().query)
    }

    /** Y sus páginas siguientes también son búsquedas, con el mismo término. */
    @Test
    fun `the term travels with every page of results`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        model.search("miel")
        testScheduler.advanceUntilIdle()

        model.loadMore()
        testScheduler.advanceUntilIdle()

        val searches = catalog.asks.filter { it.query == "miel" }
        assertEquals(listOf(1, 2), searches.map { it.page })
    }

    /**
     * Elegir un pilar **acota** la búsqueda en curso, no la cancela.
     *
     * Es lo que hace el sitio con su parámetro `pillar`: buscar «miel» en Nutrición es una pregunta,
     * y perder el término al tocar un chip obligaría a escribirlo otra vez.
     */
    @Test
    fun `choosing a pillar narrows the search instead of dropping it`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        model.search("miel")
        testScheduler.advanceUntilIdle()

        model.select(PillarType.NUTRITION)
        testScheduler.advanceUntilIdle()

        assertEquals("miel", model.state.value.query)
        val last = catalog.asks.last()
        assertEquals("miel", last.query)
        assertEquals(PillarType.NUTRITION, last.pillar)
    }

    /** Borrar el término devuelve al feed, que es otra fuente y empieza por su primera página. */
    @Test
    fun `clearing the term goes back to the feed`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        model.search("miel")
        testScheduler.advanceUntilIdle()

        model.search("")
        testScheduler.advanceUntilIdle()

        val last = catalog.asks.last()
        assertEquals("", last.query, "siguió buscando con el término borrado")
        assertEquals(1, last.page)
    }
}
