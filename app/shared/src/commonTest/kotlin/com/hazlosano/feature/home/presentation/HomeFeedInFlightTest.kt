package com.hazlosano.feature.home.presentation

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import com.hazlosano.domain.repository.CatalogRepository
import com.hazlosano.domain.usecase.GetCatalogUseCase
import com.hazlosano.domain.usecase.SearchCatalogUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lo que pasa **mientras** una petición viaja.
 *
 * Spec: `features/publicaciones_en_el_home.feature`, slice 3.
 *
 * Está aparte de `HomeFeedViewModelTest` porque necesita otro doble. Aquél contesta al instante, y
 * por eso ninguna de sus dieciséis pruebas tenía nunca una petición todavía en vuelo cuando ocurría
 * lo siguiente — que es exactamente la condición de los cuatro fallos que este slice arregla. Un
 * catálogo que contesta al instante no puede reproducir ninguno.
 */
class HomeFeedInFlightTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /**
     * Un catálogo que se queda a medias y se suelta cuando el test quiera.
     *
     * [honoursCancellation] decide de qué clase de cliente se trata, y las dos clases hacen falta:
     *
     * - **Atendiendo la cancelación** (lo normal, y lo que hace un cliente HTTP sano) la petición
     *   abandonada muere al cancelarse y su respuesta no llega nunca. Con este doble se prueba que
     *   el ViewModel de verdad cancela.
     * - **Sin atenderla** —`suspendCoroutine` y no `suspendCancellableCoroutine`— la respuesta llega
     *   igual, ya abandonada. Con este otro se prueba la guarda de frescura, que es lo único que
     *   impide entonces que dos listas se mezclen.
     */
    private class HeldCatalog(private val honoursCancellation: Boolean = true) : CatalogRepository {

        data class Ask(val pillar: PillarType?, val page: Int, val query: String)

        val asks: MutableList<Ask> = mutableListOf()

        /** Cómo contestarle a cada petición, en el orden en que se hicieron. */
        private val answers: MutableList<(CatalogPage) -> Unit> = mutableListOf()

        override suspend fun getCatalog(
            pillar: PillarType?,
            page: Int,
            pageSize: Int,
            location: VisitorLocation?,
        ): CatalogPage = hold(Ask(pillar, page, query = ""))

        override suspend fun searchCatalog(
            query: String,
            pillar: PillarType?,
            page: Int,
            pageSize: Int,
            location: VisitorLocation?,
        ): CatalogPage = hold(Ask(pillar, page, query))

        private suspend fun hold(ask: Ask): CatalogPage {
            asks += ask
            return if (honoursCancellation) {
                val gate = CompletableDeferred<CatalogPage>()
                answers += { page -> gate.complete(page) }
                gate.await()
            } else {
                suspendCoroutine { continuation -> answers += { page -> continuation.resume(page) } }
            }
        }

        /** Suelta la petición número [index] con lo que haya encontrado. */
        fun release(index: Int, publications: List<HazloProduct>, hasMore: Boolean = true) {
            answers[index](CatalogPage.Fresh(publications = publications, hasMore = hasMore))
        }

        /** Suelta la petición número [index] sin nada que enseñar y sin nada guardado. */
        fun fail(index: Int) {
            answers[index](CatalogPage.Unavailable)
        }
    }

    private fun publications(tag: String, count: Int = 3): List<HazloProduct> =
        List(count) { i ->
            HazloProduct(
                id = "$tag-$i",
                name = "Publicación $tag $i",
                description = "",
                price = null,
                imageUrl = "",
                category = "",
            )
        }

    private fun feed(catalog: HeldCatalog) = HomeFeedViewModel(
        getCatalog = GetCatalogUseCase(catalog),
        searchCatalog = SearchCatalogUseCase(catalog),
        pageSize = 20,
        typingPause = 350.milliseconds,
    )

    private fun ids(model: HomeFeedViewModel): List<String> =
        model.state.value.publications.map { it.id }

    // ─────────── Un cliente que atiende la cancelación ───────────

    /**
     * El fallo tal y como se reportó: se abre el Inicio, se escribe antes de que llegue la primera
     * página, y no se carga nada nunca más.
     *
     * Antes del arreglo esto se quedaba en una sola petición: `search` no apagaba el `loading` de la
     * página que viajaba, y la primera línea de `loadMore` lo leía y volvía sin pedir nada.
     */
    @Test
    fun `typing while the feed is loading searches anyway`() = runTest(dispatcher) {
        val catalog = HeldCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        assertEquals(1, catalog.asks.size, "la primera página no llegó a salir")

        model.search("miel")
        testScheduler.advanceUntilIdle()

        assertEquals(2, catalog.asks.size, "no se buscó: la petición en vuelo bloqueó a la siguiente")
        assertEquals("miel", catalog.asks.last().query)

        val found = publications("miel")
        catalog.release(1, found)
        testScheduler.advanceUntilIdle()

        assertFalse(model.state.value.loading, "me quedé esperando para siempre")
        assertEquals(found.map { it.id }, ids(model))
    }

    @Test
    fun `a request that comes back with results turns the indicator off`() = runTest(dispatcher) {
        val catalog = HeldCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        assertTrue(model.state.value.loading, "no se encendió al salir")

        catalog.release(0, publications("feed"))
        testScheduler.advanceUntilIdle()

        assertFalse(model.state.value.loading)
    }

    @Test
    fun `a request that fails turns the indicator off`() = runTest(dispatcher) {
        val catalog = HeldCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        catalog.fail(0)
        testScheduler.advanceUntilIdle()

        assertFalse(model.state.value.loading)
        assertTrue(model.state.value.failed)
    }

    /**
     * La tercera fila de la tabla: una petición que deja de ser la actual.
     *
     * El indicador **pasa a ser de la que la sustituye**, y se apaga cuando ésa termina. Lo que este
     * slice arregla es que antes no se apagaba nunca: la vieja volvía, se descartaba sin apagarlo, y
     * ese `loading` colgado bloqueaba todo lo demás.
     */
    @Test
    fun `a superseded request does not leave the indicator on`() = runTest(dispatcher) {
        val catalog = HeldCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        model.search("miel")
        testScheduler.advanceUntilIdle()
        catalog.release(1, publications("miel"))
        testScheduler.advanceUntilIdle()

        assertFalse(model.state.value.loading)
    }

    /**
     * Empezar otra lista **cancela** la petición que viajaba, en vez de dejarla llegar para
     * descartarla. Soltarla después no despierta a nadie.
     */
    @Test
    fun `starting another list cancels the one in flight`() = runTest(dispatcher) {
        val catalog = HeldCatalog()
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        model.search("miel")
        testScheduler.advanceUntilIdle()

        catalog.release(0, publications("feed"))
        testScheduler.advanceUntilIdle()

        assertTrue(model.state.value.publications.isEmpty(), "la petición cancelada escribió igual")
        assertEquals("miel", model.state.value.query)
    }

    // ─────────── Un cliente que NO atiende la cancelación ───────────

    /**
     * Una respuesta abandonada que llega igual no mezcla dos listas ni bloquea lo que venga después.
     *
     * Aquí es donde el doble terco gana su sitio: con uno normal la petición vieja se cancela y no
     * vuelve, y entonces esta prueba no probaría nada.
     */
    @Test
    fun `a stubborn discarded answer changes nothing and blocks nothing`() = runTest(dispatcher) {
        val catalog = HeldCatalog(honoursCancellation = false)
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        model.search("miel")
        testScheduler.advanceUntilIdle()

        // La página del feed, ya abandonada, vuelve.
        catalog.release(0, publications("feed"))
        testScheduler.advanceUntilIdle()
        assertTrue(
            model.state.value.publications.isEmpty(),
            "la respuesta abandonada se coló en la lista de la búsqueda",
        )

        catalog.release(1, publications("miel"))
        testScheduler.advanceUntilIdle()
        assertEquals(3, model.state.value.publications.size)

        // Y a partir de aquí se sigue pudiendo cargar, que es lo que antes moría.
        model.loadMore()
        testScheduler.advanceUntilIdle()
        assertEquals(3, catalog.asks.size, "la respuesta descartada dejó el feed muerto")
        assertEquals("miel", catalog.asks.last().query)
    }

    /** Y el reintento tampoco se queda bloqueado detrás de una respuesta descartada. */
    @Test
    fun `and the retry works after a discarded answer`() = runTest(dispatcher) {
        val catalog = HeldCatalog(honoursCancellation = false)
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        model.search("miel")
        testScheduler.advanceUntilIdle()
        catalog.release(0, publications("feed"))
        catalog.fail(1)
        testScheduler.advanceUntilIdle()
        assertTrue(model.state.value.failed)

        model.retry()
        testScheduler.advanceUntilIdle()

        assertEquals(3, catalog.asks.size, "el reintento no salió")
        catalog.release(2, publications("miel"))
        testScheduler.advanceUntilIdle()
        assertFalse(model.state.value.failed)
        assertFalse(model.state.value.loading)
    }

    /** El segundo término gana, y los del primero no aparecen aunque lleguen después. */
    @Test
    fun `the second term wins over the first`() = runTest(dispatcher) {
        val catalog = HeldCatalog(honoursCancellation = false)
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()
        catalog.release(0, publications("feed"))
        testScheduler.advanceUntilIdle()

        model.search("mi")
        testScheduler.advanceUntilIdle()
        model.search("miel")
        testScheduler.advanceUntilIdle()

        val fromMiel = publications("miel")
        catalog.release(2, fromMiel)
        testScheduler.advanceUntilIdle()
        // La de «mi», ya abandonada, vuelve después.
        catalog.release(1, publications("mi"))
        testScheduler.advanceUntilIdle()

        assertEquals("miel", model.state.value.query)
        assertEquals(fromMiel.map { it.id }, ids(model))
    }

    /**
     * Lo que vuelve se escribe sobre lo que hay, no sobre la foto de antes de salir.
     *
     * Antes, la última línea de `loadMore` era `current.copy(...)` sobre el estado capturado
     * **antes** de la petición. Una respuesta tardía devolvía el app a como estaba cuando aquélla
     * arrancó: la lista vieja y el filtro viejo.
     */
    @Test
    fun `what comes back is written over what is there now`() = runTest(dispatcher) {
        val catalog = HeldCatalog(honoursCancellation = false)
        val model = feed(catalog)
        testScheduler.advanceUntilIdle()

        model.select(PillarType.SLEEP)
        testScheduler.advanceUntilIdle()

        val fromSleep = publications("sueno")
        catalog.release(1, fromSleep)
        testScheduler.advanceUntilIdle()

        // La página sin filtro, abandonada al tocar el chip, vuelve ahora.
        catalog.release(0, publications("todos"))
        testScheduler.advanceUntilIdle()

        assertEquals(PillarType.SLEEP, model.state.value.filter, "volvió al filtro de antes")
        assertEquals(
            fromSleep.map { it.id },
            ids(model),
            "resucitó la lista que había antes de la petición",
        )
    }
}
