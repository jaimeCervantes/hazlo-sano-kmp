package com.hazlosano.feature.home.presentation

import com.hazlosano.data.catalog.FakeCatalog
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.usecase.GetCatalogUseCase
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

/**
 * El feed del Inicio: acumular páginas sin mezclar filtros y sin pedir dos veces lo mismo.
 *
 * Spec: `features/publicaciones_en_el_home.feature`, slice 1.
 *
 * Lo que ya **no** se prueba aquí es buscar por texto: el feed dejó de saber qué es un término al
 * mudarse la búsqueda a la barra de arriba. Esas seis pruebas están en `SearchViewModelTest`, y la
 * mecánica que las dos comparten en `PublicationPagerTest`.
 */
class HomeFeedViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun feed(catalog: FakeCatalog) = HomeFeedViewModel(
        getCatalog = GetCatalogUseCase(catalog),
        pageSize = 20,
    )

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
}
