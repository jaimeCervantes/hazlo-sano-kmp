package com.hazlosano.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.CatalogRepository
import com.hazlosano.domain.usecase.GetCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
 */
class HomeFeedViewModel(
    private val getCatalog: GetCatalogUseCase,
    private val pageSize: Int = CatalogRepository.DEFAULT_PAGE_SIZE,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeFeedUiState())
    val state: StateFlow<HomeFeedUiState> = _state.asStateFlow()

    /** La última página que se pidió con éxito. La siguiente es esta más una. */
    private var loadedPage = 0

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
     */
    fun select(pillar: PillarType?) {
        if (pillar == _state.value.filter) return
        loadedPage = 0
        _state.value = HomeFeedUiState(filter = pillar)
        loadMore()
    }

    /**
     * Pide la página siguiente, si es que hay y si no se está pidiendo ya otra.
     *
     * Las dos guardas importan por lo mismo: la pantalla llama a esto **al llegar al final**, y ese
     * final se alcanza varias veces mientras una petición está en vuelo. Sin ellas, un scroll
     * decidido dispara media docena de peticiones y las publicaciones llegan repetidas.
     */
    fun loadMore() {
        val current = _state.value
        if (current.loading || !current.hasMore) return

        val filter = current.filter
        val page = loadedPage + 1
        _state.value = current.copy(loading = true, failed = false)

        viewModelScope.launch {
            val result = getCatalog(pillar = filter, page = page, pageSize = pageSize)

            // El filtro pudo cambiar mientras esto viajaba, y entonces lo que llegó es de otra
            // lista. Descartarlo es lo correcto: `select` ya dejó pedida la primera página del
            // filtro nuevo, y añadir esto encima mezclaría los dos.
            if (_state.value.filter != filter) return@launch

            _state.value = when (result) {
                is CatalogPage.Fresh -> {
                    loadedPage = page
                    current.copy(
                        publications = current.publications + result.publications,
                        loading = false,
                        hasMore = result.hasMore,
                        stale = false,
                        firstPagePending = false,
                    )
                }

                is CatalogPage.Cached -> {
                    loadedPage = page
                    current.copy(
                        publications = current.publications + result.publications,
                        loading = false,
                        // Lo guardado se acaba donde se acabó de guardar: seguir pidiendo páginas de
                        // una caché que ya se agotó es pedirle al disco lo que no tiene.
                        hasMore = result.publications.size >= pageSize,
                        stale = true,
                        firstPagePending = false,
                    )
                }

                // Nada que enseñar y nada guardado. **No se vacía lo ya cargado**: quien lleva tres
                // páginas leídas y pierde la red se queda con sus tres páginas y un aviso, no con
                // una pantalla en blanco.
                CatalogPage.Unavailable -> current.copy(
                    loading = false,
                    failed = true,
                    hasMore = false,
                )
            }
        }
    }

    /** Reintenta lo que falló, sin tocar lo que ya se había leído. */
    fun retry() {
        if (_state.value.failed) {
            _state.value = _state.value.copy(hasMore = true, failed = false)
            loadMore()
        }
    }
}
