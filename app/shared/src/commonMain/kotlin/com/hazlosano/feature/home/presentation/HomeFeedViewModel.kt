package com.hazlosano.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.CatalogRepository
import com.hazlosano.domain.usecase.GetCatalogUseCase
import com.hazlosano.domain.usecase.SearchCatalogUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * El feed del Inicio: lo que el proyecto publica, con un pilar puesto o sin ninguno.
 *
 * [filter] nulo es «Todos», que es exactamente lo que el sitio entiende por no mandar el parámetro.
 * No hay un `PillarType.ALL` inventado: el filtro que no filtra es la ausencia de filtro.
 *
 * [stale] dice que lo que se ve salió de la caché porque no hubo red. Se guarda aparte de las
 * publicaciones para que la pantalla pueda decirlo sin que la lista cambie de forma.
 */
data class HomeFeedUiState(
    val filter: PillarType? = null,
    /**
     * Lo que se está buscando, o vacío cuando se está mirando el feed.
     *
     * **Forma parte de lo que se pide y no sólo de lo que se ve**, porque el sitio busca de verdad:
     * con término, las páginas salen de `/api/search` y no del feed. Filtrar en el dispositivo lo ya
     * cargado habría dicho «no hay» sobre veinte publicaciones, que no es una respuesta sobre el
     * catálogo.
     */
    val query: String = "",
    val publications: List<HazloProduct> = emptyList(),
    val loading: Boolean = false,
    val hasMore: Boolean = true,
    val stale: Boolean = false,
    /** Ni una página cargada todavía: la primera petición aún no ha vuelto. */
    val firstPagePending: Boolean = true,
    /** La última petición falló y no había nada que enseñar en su lugar. */
    val failed: Boolean = false,
)

/**
 * Acumula las páginas del feed y recuerda con qué filtro se pidieron.
 *
 * **Acumula aquí y no en la pantalla** porque sobrevivir a la navegación es justamente lo que se
 * necesita: bajar veinte publicaciones, entrar a un pilar y volver no puede costar volver a
 * bajarlas. Es la misma lección que dejó el indicador de importación de rutas, que se perdía al
 * navegar porque su ViewModel se construía dentro de una rama que la navegación desmonta.
 *
 * **Hay como mucho una petición en vuelo, y es cancelable.** Ésa es la regla de la que salen las
 * tres garantías que antes se incumplían: nadie empieza una segunda mientras viaja la primera, quien
 * empieza una lista nueva cancela la anterior en vez de dejarla llegar para descartarla, y lo que
 * vuelve se escribe sobre el estado que hay **al volver** y no sobre la foto de antes de salir.
 */
class HomeFeedViewModel(
    private val getCatalog: GetCatalogUseCase,
    private val searchCatalog: SearchCatalogUseCase,
    private val pageSize: Int = CatalogRepository.DEFAULT_PAGE_SIZE,
    /**
     * Cuánto se espera desde la última tecla antes de preguntarle al sitio.
     *
     * Sin esta espera, escribir «miel» son cuatro búsquedas de las que tres sobran, y llegan
     * desordenadas. Entra por constructor para que un test no tenga que esperar de verdad.
     */
    private val typingPause: Duration = TYPING_PAUSE,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeFeedUiState())
    val state: StateFlow<HomeFeedUiState> = _state.asStateFlow()

    /** La última página que se pidió con éxito. La siguiente es esta más una. */
    private var loadedPage = 0

    /**
     * Lo que está viajando ahora mismo, si hay algo.
     *
     * **La espera al teclear y la ida a la red son la misma petición**, y por eso son un solo
     * trabajo. Antes eran dos: se cancelaba la espera y la corrutina de red seguía viva, así que la
     * respuesta de un término abandonado llegaba igual y había que descartarla al llegar.
     */
    private var inFlight: Job? = null

    init {
        loadMore()
    }

    /**
     * Cambia el filtro y **empieza de nuevo**.
     *
     * Conservar lo cargado al cambiar de pilar mezclaría dos filtros en una lista, que es peor que
     * esperar: lo que se ve dejaría de corresponder a lo que el chip dice.
     *
     * Elegir el filtro que ya está puesto no hace nada: sería tirar tres páginas para volver a
     * pedirlas iguales.
     *
     * El término se conserva: elegir un pilar acota la búsqueda que se está haciendo, no la cancela
     * — que es lo que hace el sitio con su parámetro `pillar`.
     */
    fun select(pillar: PillarType?) {
        if (pillar == _state.value.filter) return
        restart(filter = pillar, query = _state.value.query, wait = Duration.ZERO)
    }

    /**
     * Lo que se escribe, que no se pregunta en el acto.
     *
     * El término se guarda ya —para que lo escrito aparezca sin retraso— pero la petición espera a
     * que se deje de teclear. Cancelar la anterior es lo que impide que la respuesta de «mie» llegue
     * después de la de «miel» y la pise.
     */
    fun search(text: String) {
        if (text == _state.value.query) return
        restart(filter = _state.value.filter, query = text, wait = typingPause)
    }

    /**
     * Pide la página siguiente, si es que hay y si no se está pidiendo ya otra.
     *
     * Las dos guardas importan por lo mismo: la pantalla llama a esto **al llegar al final**, y ese
     * final se alcanza varias veces mientras una petición está en vuelo. Sin ellas, un scroll
     * decidido dispara media docena de peticiones y las publicaciones llegan repetidas.
     *
     * Que la guarda de `loading` esté **sólo aquí** y no dentro de [ask] es lo que arregla el fallo
     * que dejaba el Inicio muerto: empezar una lista nueva pasa por [restart], que no le pide
     * permiso a una petición que acaba de cancelar.
     */
    fun loadMore() {
        val current = _state.value
        if (current.loading || !current.hasMore) return
        ask(wait = Duration.ZERO)
    }

    /** Reintenta lo que falló, sin tocar lo que ya se había leído. */
    fun retry() {
        if (!_state.value.failed) return
        _state.value = _state.value.copy(hasMore = true, failed = false)
        loadMore()
    }

    /**
     * Empieza una lista nueva: cancela lo que viajaba y pide su primera página.
     *
     * Cambiar de filtro y cambiar de término son la misma operación —otra lista desde su página 1— y
     * por eso comparten esto. Cuando eran dos caminos separados, uno de los dos se olvidaba de
     * apagar el indicador de la petición que acababa de abandonar.
     */
    private fun restart(filter: PillarType?, query: String, wait: Duration) {
        inFlight?.cancel()
        loadedPage = 0
        _state.value = HomeFeedUiState(filter = filter, query = query)
        ask(wait)
    }

    /**
     * Sale a pedir una página, tras esperar [wait] si hay que esperar.
     *
     * No comprueba si ya hay otra en vuelo: eso es cosa de quien llama. [loadMore] lo comprueba
     * porque la pantalla lo dispara sin querer; [restart] no, porque acaba de cancelar la anterior.
     */
    private fun ask(wait: Duration) {
        val from = _state.value
        val filter = from.filter
        val query = from.query
        val page = loadedPage + 1
        _state.value = from.copy(loading = true, failed = false)

        inFlight = viewModelScope.launch {
            delay(wait)

            // Con término se le pregunta al buscador del sitio; sin él, al feed. Son dos endpoints
            // con dos contratos, y por eso son dos casos de uso: ver `CatalogRepository`.
            val result = if (query.isBlank()) {
                getCatalog(pillar = filter, page = page, pageSize = pageSize)
            } else {
                searchCatalog(query = query, pillar = filter, page = page, pageSize = pageSize)
            }

            // **Sobre el estado que hay ahora, no sobre `from`.** `from` es la foto de antes de
            // salir: escribir sobre ella devolvería el app a como estaba cuando la petición
            // arrancó, pisando lo que hubiera cambiado mientras viajaba.
            val now = _state.value

            // Quien cambia de lista cancela esta petición, así que normalmente no se llega aquí con
            // una respuesta ajena. Se comprueba igual porque una suspensión que no atienda la
            // cancelación dejaría llegar la respuesta de otra lista, y añadirla mezclaría las dos.
            if (now.filter != filter || now.query != query) return@launch

            if (result !is CatalogPage.Unavailable) loadedPage = page
            _state.value = now.plus(result)
        }
    }

    /** Lo que llegó, encima de lo que ya había. */
    private fun HomeFeedUiState.plus(result: CatalogPage): HomeFeedUiState = when (result) {
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
            // Lo guardado se acaba donde se acabó de guardar: seguir pidiendo páginas de una caché
            // que ya se agotó es pedirle al disco lo que no tiene.
            hasMore = result.publications.size >= pageSize,
            stale = true,
            firstPagePending = false,
        )

        // Nada que enseñar y nada guardado. **No se vacía lo ya cargado**: quien lleva tres páginas
        // leídas y pierde la red se queda con sus tres páginas y un aviso, no con una pantalla en
        // blanco.
        CatalogPage.Unavailable -> copy(
            loading = false,
            failed = true,
            hasMore = false,
        )
    }

    private companion object {
        /**
         * Bastante para que una palabra escrita del tirón sea una sola búsqueda, y poco para que no
         * se note al parar.
         */
        val TYPING_PAUSE: Duration = 350.milliseconds
    }
}
