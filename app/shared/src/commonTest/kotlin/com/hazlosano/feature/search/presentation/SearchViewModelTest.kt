package com.hazlosano.feature.search.presentation

import com.hazlosano.data.catalog.FakeCatalog
import com.hazlosano.domain.model.PillarType
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
 * La búsqueda de la barra de arriba.
 *
 * Spec: `features/busqueda_en_la_barra.feature`, slice 1, y los `@slice-2` de
 * `publicaciones_en_el_home.feature`, que siguen valiendo sobre la pantalla nueva.
 *
 * Lo que aquí no se prueba —acumular páginas, no pedir dos a la vez, cancelar la que viajaba— vive
 * en `PublicationPagerTest`, que es donde se mudó al separarse del feed.
 */
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun search(catalog: FakeCatalog) = SearchViewModel(
        searchCatalog = SearchCatalogUseCase(catalog),
        pageSize = 20,
        typingPause = 350.milliseconds,
    )

    /** Nada más nacer no ha preguntado nada: la búsqueda empieza cuando alguien escribe. */
    @Test
    fun `it asks for nothing until something is typed`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = search(catalog)
        testScheduler.advanceUntilIdle()

        assertTrue(catalog.asks.isEmpty(), "buscó sin que nadie hubiera escrito")
        assertEquals("", model.query.value)
        assertTrue(model.results.value.publications.isEmpty())
    }

    /**
     * Escribir no dispara una petición por tecla.
     *
     * Sin la espera, «miel» son cuatro búsquedas de las que tres sobran — y llegan desordenadas, así
     * que la de «mie» puede pisar a la de «miel».
     */
    @Test
    fun `typing a word is one search and not one per letter`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = search(catalog)

        model.type("m")
        model.type("mi")
        model.type("mie")
        model.type("miel")
        testScheduler.advanceUntilIdle()

        assertEquals(1, catalog.asks.size, "pidió una búsqueda por tecla")
        assertEquals("miel", catalog.asks.single().query)
    }

    /**
     * Lo escrito aparece ya, aunque la petición espere.
     *
     * De esto depende que la pantalla de resultados se abra con la primera letra: está abierta
     * exactamente cuando el término no está en blanco.
     */
    @Test
    fun `what is typed shows up before the request goes out`() = runTest(dispatcher) {
        val model = search(FakeCatalog())

        model.type("m")

        assertEquals("m", model.query.value)
    }

    /** Se busca en los cuatro pilares: entrar por una pestaña no acota nada. */
    @Test
    fun `a search starts with no pillar`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = search(catalog)

        model.type("miel")
        testScheduler.advanceUntilIdle()

        assertEquals(null, catalog.asks.single().pillar)
        assertEquals(null, model.results.value.filter)
    }

    /** Y sus páginas siguientes también son búsquedas, con el mismo término. */
    @Test
    fun `the term travels with every page of results`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = search(catalog)
        model.type("miel")
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
        val model = search(catalog)
        model.type("miel")
        testScheduler.advanceUntilIdle()

        model.select(PillarType.NUTRITION)
        testScheduler.advanceUntilIdle()

        assertEquals("miel", model.query.value)
        val last = catalog.asks.last()
        assertEquals("miel", last.query)
        assertEquals(PillarType.NUTRITION, last.pillar)
        assertEquals(1, last.page, "acotar no empezó por la primera página")
    }

    /** El chip que ya está puesto no tira los resultados para volver a pedirlos iguales. */
    @Test
    fun `choosing the pillar that is already on does nothing`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = search(catalog)
        model.type("miel")
        testScheduler.advanceUntilIdle()
        val asksBefore = catalog.asks.size

        model.select(null)
        testScheduler.advanceUntilIdle()

        assertEquals(asksBefore, catalog.asks.size)
    }

    /**
     * Borrar lo escrito cierra la búsqueda, y no busca la cadena vacía.
     *
     * Es la misma operación que la flecha de la barra y el gesto del sistema, y de ella depende que
     * la pantalla de resultados se cierre: está abierta cuando hay término.
     */
    @Test
    fun `clearing the term closes the search instead of searching for nothing`() =
        runTest(dispatcher) {
            val catalog = FakeCatalog()
            val model = search(catalog)
            model.type("miel")
            testScheduler.advanceUntilIdle()
            val asksBefore = catalog.asks.size

            model.type("")
            testScheduler.advanceUntilIdle()

            assertEquals("", model.query.value)
            assertEquals(asksBefore, catalog.asks.size, "buscó la cadena vacía")
            assertTrue(model.results.value.publications.isEmpty())
        }

    /** Y cerrar devuelve el chip a «Todos»: la siguiente búsqueda no hereda el pilar de la anterior. */
    @Test
    fun `closing the search forgets the pillar too`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        val model = search(catalog)
        model.type("miel")
        testScheduler.advanceUntilIdle()
        model.select(PillarType.NUTRITION)
        testScheduler.advanceUntilIdle()

        model.clear()
        model.type("avena")
        testScheduler.advanceUntilIdle()

        assertEquals(null, catalog.asks.last().pillar, "la búsqueda nueva heredó el pilar de la vieja")
        assertEquals(null, model.results.value.filter)
    }

    /**
     * Sin red, una búsqueda no tiene respuesta y se dice.
     *
     * **No se contesta con lo guardado**: la caché guarda publicaciones, no búsquedas, y filtrar por
     * texto en el dispositivo sería una segunda implementación del buscador con reglas peores.
     */
    @Test
    fun `without network a search has no answer`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        catalog.failFrom = 1
        val model = search(catalog)

        model.type("miel")
        testScheduler.advanceUntilIdle()

        assertTrue(model.results.value.failed)
        assertFalse(model.results.value.stale, "contestó con lo guardado a una pregunta por texto")
        assertTrue(model.results.value.publications.isEmpty())
    }

    /** Y se puede reintentar. */
    @Test
    fun `and it can be retried`() = runTest(dispatcher) {
        val catalog = FakeCatalog()
        catalog.failFrom = 1
        val model = search(catalog)
        model.type("miel")
        testScheduler.advanceUntilIdle()

        catalog.failFrom = null
        model.retry()
        testScheduler.advanceUntilIdle()

        assertFalse(model.results.value.failed)
        assertEquals(20, model.results.value.publications.size)
    }
}
