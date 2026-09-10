package com.hazlosano.core.presentation

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
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
 * Estas nueve pruebas nacieron contra `HomeFeedViewModel` y se mudaron aquí con la mecánica que
 * afirmaban. No se copiaron: en el ViewModel ya no queda nada de esto que probar.
 *
 * Hacía falta otro doble que el de los ViewModels, que contestan al instante — y por eso ninguna de
 * las pruebas que había tenía nunca una petición todavía en vuelo cuando ocurría lo siguiente, que
 * es exactamente la condición de los cuatro fallos que este slice arregló.
 */
class PublicationPagerTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /**
     * Páginas que se quedan a medias y se sueltan cuando el test quiera.
     *
     * [honoursCancellation] decide de qué clase de cliente se trata, y las dos clases hacen falta:
     *
     * - **Atendiendo la cancelación** (lo normal, y lo que hace un cliente HTTP sano) la petición
     *   abandonada muere al cancelarse y su respuesta no llega nunca. Con este doble se prueba que
     *   el paginador de verdad cancela.
     * - **Sin atenderla** —`suspendCoroutine` y no `suspendCancellableCoroutine`— la respuesta llega
     *   igual, ya abandonada. Con este otro se prueba la guarda de generación, que es lo único que
     *   impide entonces que dos listas se mezclen.
     */
    private class HeldPages(private val honoursCancellation: Boolean = true) {

        data class Ask(val page: Int, val pageSize: Int)

        val asks: MutableList<Ask> = mutableListOf()

        /** Cómo contestarle a cada petición, en el orden en que se hicieron. */
        private val answers: MutableList<(CatalogPage) -> Unit> = mutableListOf()

        suspend fun fetch(page: Int, pageSize: Int): CatalogPage {
            asks += Ask(page, pageSize)
            return if (honoursCancellation) {
                val gate = CompletableDeferred<CatalogPage>()
                answers += { result -> gate.complete(result) }
                gate.await()
            } else {
                suspendCoroutine { continuation ->
                    answers += { result -> continuation.resume(result) }
                }
            }
        }

        fun release(index: Int, publications: List<HazloProduct>, hasMore: Boolean = true) {
            answers[index](CatalogPage.Fresh(publications = publications, hasMore = hasMore))
        }

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

    private fun ids(pager: PublicationPager): List<String> =
        pager.state.value.publications.map { it.id }

    /**
     * Un paginador sobre un scope propio, no sobre el del test.
     *
     * Tiene que ser propio porque **casi ninguna de estas pruebas suelta lo que retuvo**: una
     * petición que se queda a medias a propósito dejaría a `runTest` esperándola para siempre si
     * colgara del test. Es el mismo sitio que ocupa `viewModelScope` en las pruebas de los
     * ViewModels — el mismo planificador, otro dueño.
     */
    private fun pager(): PublicationPager =
        PublicationPager(CoroutineScope(dispatcher), pageSize = 20)

    // ─────────── Un cliente que atiende la cancelación ───────────

    /**
     * El fallo tal y como se reportó: se abre el Inicio, se escribe antes de que llegue la primera
     * página, y no se carga nada nunca más.
     *
     * Antes del arreglo, la lista nueva le pedía permiso al `loading` de la que viajaba y se
     * quedaba sin salir.
     */
    @Test
    fun `starting another list while one is in flight asks anyway`() = runTest(dispatcher) {
        val pages = HeldPages()
        val pager = pager()

        pager.restart(filter = null) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()
        assertEquals(1, pages.asks.size, "la primera página no llegó a salir")

        pager.restart(filter = PillarType.NUTRITION) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()
        assertEquals(2, pages.asks.size, "la petición en vuelo bloqueó a la que venía a sustituirla")

        val found = publications("nutricion")
        pages.release(1, found)
        testScheduler.advanceUntilIdle()

        assertFalse(pager.state.value.loading, "me quedé esperando para siempre")
        assertEquals(found.map { it.id }, ids(pager))
    }

    @Test
    fun `a request that comes back with results turns the indicator off`() = runTest(dispatcher) {
        val pages = HeldPages()
        val pager = pager()

        pager.restart(filter = null) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()
        assertTrue(pager.state.value.loading, "no se encendió al salir")

        pages.release(0, publications("feed"))
        testScheduler.advanceUntilIdle()

        assertFalse(pager.state.value.loading)
        assertFalse(pager.state.value.firstPagePending)
    }

    @Test
    fun `a request that fails turns the indicator off`() = runTest(dispatcher) {
        val pages = HeldPages()
        val pager = pager()

        pager.restart(filter = null) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()

        pages.fail(0)
        testScheduler.advanceUntilIdle()

        assertFalse(pager.state.value.loading)
        assertTrue(pager.state.value.failed)
    }

    /**
     * La tercera fila de la tabla: una petición que deja de ser la actual.
     *
     * El indicador **pasa a ser de la que la sustituye**, y se apaga cuando ésa termina. Lo que este
     * slice arregló es que antes no se apagaba nunca: la vieja volvía, se descartaba sin apagarlo, y
     * ese `loading` colgado bloqueaba todo lo demás.
     */
    @Test
    fun `a superseded request does not leave the indicator on`() = runTest(dispatcher) {
        val pages = HeldPages()
        val pager = pager()

        pager.restart(filter = null) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()
        pager.restart(filter = PillarType.SLEEP) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()

        pages.release(1, publications("sueno"))
        testScheduler.advanceUntilIdle()

        assertFalse(pager.state.value.loading)
    }

    /**
     * Empezar otra lista **cancela** la que viajaba, en vez de dejarla llegar para descartarla.
     * Soltarla después no despierta a nadie.
     */
    @Test
    fun `starting another list cancels the one in flight`() = runTest(dispatcher) {
        val pages = HeldPages()
        val pager = pager()

        pager.restart(filter = null) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()
        pager.restart(filter = PillarType.SLEEP) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()

        pages.release(0, publications("todos"))
        testScheduler.advanceUntilIdle()

        assertTrue(pager.state.value.publications.isEmpty(), "la petición cancelada escribió igual")
        assertEquals(PillarType.SLEEP, pager.state.value.filter)
    }

    /** Y cerrar la lista del todo tampoco deja nada vivo detrás. */
    @Test
    fun `clearing cancels what was in flight and leaves nothing`() = runTest(dispatcher) {
        val pages = HeldPages()
        val pager = pager()

        pager.restart(filter = PillarType.MIND) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()

        pager.clear()
        pages.release(0, publications("mente"))
        testScheduler.advanceUntilIdle()

        assertTrue(pager.state.value.publications.isEmpty())
        assertEquals(null, pager.state.value.filter)
        assertFalse(pager.state.value.loading)
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
        val pages = HeldPages(honoursCancellation = false)
        val pager = pager()

        pager.restart(filter = null) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()
        pager.restart(filter = PillarType.SLEEP) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()

        // La página de la lista vieja, ya abandonada, vuelve.
        pages.release(0, publications("todos"))
        testScheduler.advanceUntilIdle()
        assertTrue(
            pager.state.value.publications.isEmpty(),
            "la respuesta abandonada se coló en la lista nueva",
        )

        pages.release(1, publications("sueno"))
        testScheduler.advanceUntilIdle()
        assertEquals(3, pager.state.value.publications.size)

        // Y a partir de aquí se sigue pudiendo cargar, que es lo que antes moría.
        pager.loadMore()
        testScheduler.advanceUntilIdle()
        assertEquals(3, pages.asks.size, "la respuesta descartada dejó la lista muerta")
        assertEquals(2, pages.asks.last().page)
    }

    /** Y el reintento tampoco se queda bloqueado detrás de una respuesta descartada. */
    @Test
    fun `and the retry works after a discarded answer`() = runTest(dispatcher) {
        val pages = HeldPages(honoursCancellation = false)
        val pager = pager()

        pager.restart(filter = null) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()
        pager.restart(filter = PillarType.SLEEP) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()

        pages.release(0, publications("todos"))
        pages.fail(1)
        testScheduler.advanceUntilIdle()
        assertTrue(pager.state.value.failed)

        pager.retry()
        testScheduler.advanceUntilIdle()

        assertEquals(3, pages.asks.size, "el reintento no salió")
        pages.release(2, publications("sueno"))
        testScheduler.advanceUntilIdle()
        assertFalse(pager.state.value.failed)
        assertFalse(pager.state.value.loading)
    }

    /** La segunda lista gana, y la primera no aparece aunque llegue después. */
    @Test
    fun `the second list wins over the first`() = runTest(dispatcher) {
        val pages = HeldPages(honoursCancellation = false)
        val pager = pager()

        pager.restart(filter = PillarType.NUTRITION) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()
        pager.restart(filter = PillarType.MOVEMENT) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()

        val fromMovement = publications("movimiento")
        pages.release(1, fromMovement)
        testScheduler.advanceUntilIdle()
        // La de Nutrición, ya abandonada, vuelve después.
        pages.release(0, publications("nutricion"))
        testScheduler.advanceUntilIdle()

        assertEquals(PillarType.MOVEMENT, pager.state.value.filter)
        assertEquals(fromMovement.map { it.id }, ids(pager))
    }

    /**
     * Lo que vuelve se escribe sobre lo que hay, no sobre la foto de antes de salir.
     *
     * Antes, la última línea de `loadMore` era `current.copy(...)` sobre el estado capturado
     * **antes** de la petición. Una respuesta tardía devolvía la lista a como estaba cuando aquélla
     * arrancó.
     */
    @Test
    fun `what comes back is written over what is there now`() = runTest(dispatcher) {
        val pages = HeldPages(honoursCancellation = false)
        val pager = pager()

        pager.restart(filter = null) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()
        pager.restart(filter = PillarType.SLEEP) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceUntilIdle()

        val fromSleep = publications("sueno")
        pages.release(1, fromSleep)
        testScheduler.advanceUntilIdle()

        // La página sin filtro, abandonada al cambiar de lista, vuelve ahora.
        pages.release(0, publications("todos"))
        testScheduler.advanceUntilIdle()

        assertEquals(PillarType.SLEEP, pager.state.value.filter, "volvió al filtro de antes")
        assertEquals(
            fromSleep.map { it.id },
            ids(pager),
            "resucitó la lista que había antes de la petición",
        )
    }

    // ─────────── La espera al teclear, que es de la misma petición ───────────

    /**
     * Con espera, la petición **no sale hasta que pasa**, y empezar otra lista mientras tanto la
     * cancela sin que haya llegado a salir. Que la espera y la ida a la red sean el mismo trabajo es
     * lo que lo permite: cuando eran dos, cancelar la espera dejaba viva la corrutina de red.
     */
    @Test
    fun `a waiting request can be cancelled before it ever goes out`() = runTest(dispatcher) {
        val pages = HeldPages()
        val pager = pager()

        pager.restart(filter = null, wait = 350.milliseconds) { page, size -> pages.fetch(page, size) }
        testScheduler.advanceTimeBy(100)
        assertTrue(pages.asks.isEmpty(), "salió sin esperar")

        pager.restart(filter = PillarType.MIND, wait = 350.milliseconds) { page, size ->
            pages.fetch(page, size)
        }
        testScheduler.advanceUntilIdle()

        assertEquals(1, pages.asks.size, "salieron las dos: la primera no se canceló a tiempo")
    }
}
