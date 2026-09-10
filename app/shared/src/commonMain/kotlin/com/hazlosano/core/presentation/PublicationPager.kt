package com.hazlosano.core.presentation

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.CatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Una lista de publicaciones que se pide por páginas.
 *
 * [stale] dice que lo que se ve salió de la caché porque no hubo red. Se guarda aparte de las
 * publicaciones para que la pantalla pueda decirlo sin que la lista cambie de forma.
 */
data class PublicationListUiState(
    val filter: PillarType? = null,
    val publications: List<HazloProduct> = emptyList(),
    val loading: Boolean = false,
    val hasMore: Boolean = true,
    val stale: Boolean = false,
    /** Ni una página cargada todavía: la primera petición aún no ha vuelto. */
    val firstPagePending: Boolean = true,
    /** La última petición falló y no había nada que enseñar en su lugar. */
    val failed: Boolean = false,
)

/** De dónde sale una página. La pide quien sabe qué lista es; el paginador sólo sabe cuándo. */
typealias FetchPublicationPage = suspend (page: Int, pageSize: Int) -> CatalogPage

/**
 * Acumula páginas de publicaciones, con **una sola petición en vuelo y cancelable**.
 *
 * Sale de `HomeFeedViewModel`, que hacía esto para el feed y para la búsqueda a la vez decidiendo
 * por el término a qué endpoint preguntar. Al separarse las dos pantallas, esa mecánica —las guardas,
 * la cancelación, la acumulación, el reintento— era lo único que de verdad compartían: copiarla
 * habría dejado dos sitios donde volver a arreglar los cuatro fallos que costó entender una vez.
 *
 * Lo que cambia entre una lista y otra es **de dónde sale una página**, y eso entra por [restart]
 * como una función. El paginador no sabe si detrás hay un feed o un buscador.
 */
class PublicationPager(
    private val scope: CoroutineScope,
    private val pageSize: Int = CatalogRepository.DEFAULT_PAGE_SIZE,
) {

    private val _state = MutableStateFlow(PublicationListUiState())
    val state: StateFlow<PublicationListUiState> = _state.asStateFlow()

    /** La última página que se pidió con éxito. La siguiente es esta más una. */
    private var loadedPage = 0

    /**
     * Lo que está viajando ahora mismo, si hay algo.
     *
     * **La espera al teclear y la ida a la red son la misma petición**, y por eso son un solo
     * trabajo. Cuando eran dos, cancelar la espera dejaba viva la corrutina de red y su respuesta
     * llegaba igual, ya sin dueño.
     */
    private var inFlight: Job? = null

    /**
     * Qué lista se está pidiendo.
     *
     * Una respuesta de otra generación ya no es de nadie. Comparar generaciones en vez de comparar
     * el filtro y el término deja al paginador sin saber qué distingue a una lista de otra, que es
     * justamente lo que no le toca saber.
     */
    private var generation = 0

    private var fetch: FetchPublicationPage? = null

    /**
     * Empieza una lista nueva: cancela lo que viajaba y pide su primera página.
     *
     * [wait] es para la espera al teclear. El estado se rehace **ya** —para que lo escrito aparezca
     * sin retraso— y sólo la petición espera.
     */
    fun restart(filter: PillarType?, wait: Duration = Duration.ZERO, fetch: FetchPublicationPage) {
        inFlight?.cancel()
        generation++
        loadedPage = 0
        this.fetch = fetch
        _state.value = PublicationListUiState(filter = filter)
        ask(wait)
    }

    /**
     * Deja de haber lista: cancela lo que viajaba y se olvida de dónde salían las páginas.
     *
     * No es un [restart] con las manos vacías. Reiniciar deja pedida una primera página; esto deja
     * al paginador sin nada que pedir, que es lo que hace falta cuando la pantalla que lo miraba se
     * cierra — al borrar el término de la búsqueda, por ejemplo.
     */
    fun clear() {
        inFlight?.cancel()
        generation++
        loadedPage = 0
        fetch = null
        _state.value = PublicationListUiState()
    }

    /**
     * Pide la página siguiente, si es que hay y si no se está pidiendo ya otra.
     *
     * Las dos guardas importan por lo mismo: la pantalla llama a esto **al llegar al final**, y ese
     * final se alcanza varias veces mientras una petición está en vuelo. Sin ellas, un scroll
     * decidido dispara media docena de peticiones y las publicaciones llegan repetidas.
     *
     * Que la guarda de `loading` esté **sólo aquí** y no dentro de [ask] es lo que impide que una
     * petición en vuelo bloquee a la lista que viene a sustituirla: [restart] no le pide permiso a
     * algo que acaba de cancelar.
     */
    fun loadMore() {
        val current = _state.value
        if (current.loading || !current.hasMore) return
        ask(Duration.ZERO)
    }

    /** Reintenta lo que falló, sin tocar lo que ya se había leído. */
    fun retry() {
        if (!_state.value.failed) return
        _state.value = _state.value.copy(hasMore = true, failed = false)
        loadMore()
    }

    private fun ask(wait: Duration) {
        val fetchPage = fetch ?: return
        val mine = generation
        val page = loadedPage + 1
        _state.value = _state.value.copy(loading = true, failed = false)

        inFlight = scope.launch {
            delay(wait)
            val result = fetchPage(page, pageSize)

            // Quien empieza otra lista cancela esta petición, así que normalmente no se llega aquí
            // con una respuesta ajena. Se comprueba igual porque una suspensión que no atienda la
            // cancelación dejaría llegar la de otra generación, y añadirla mezclaría dos listas.
            if (mine != generation) return@launch

            if (result !is CatalogPage.Unavailable) loadedPage = page

            // **Sobre el estado que hay ahora, no sobre una foto de antes de salir.** Escribir sobre
            // la de antes devolvería la lista a como estaba cuando la petición arrancó.
            _state.value = _state.value.plus(result)
        }
    }

    /** Lo que llegó, encima de lo que ya había. */
    private fun PublicationListUiState.plus(result: CatalogPage): PublicationListUiState =
        when (result) {
            is CatalogPage.Fresh -> copy(
                publications = publications + result.publications,
                loading = false,
                hasMore = result.hasMore,
                stale = false,
                firstPagePending = false,
            )

            is CatalogPage.Cached -> copy(
                publications = publications + result.publications,
                loading = false,
                // Lo guardado se acaba donde se acabó de guardar: seguir pidiendo páginas de una
                // caché que ya se agotó es pedirle al disco lo que no tiene.
                hasMore = result.publications.size >= pageSize,
                stale = true,
                firstPagePending = false,
            )

            // Nada que enseñar y nada guardado. **No se vacía lo ya cargado**: quien lleva tres
            // páginas leídas y pierde la red se queda con sus tres páginas y un aviso, no con una
            // pantalla en blanco.
            CatalogPage.Unavailable -> copy(
                loading = false,
                failed = true,
                hasMore = false,
            )
        }
}
