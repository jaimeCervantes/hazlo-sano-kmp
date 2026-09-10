package com.hazlosano.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.core.presentation.PublicationListUiState
import com.hazlosano.core.presentation.PublicationPager
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.CatalogRepository
import com.hazlosano.domain.usecase.GetCatalogUseCase
import kotlinx.coroutines.flow.StateFlow

/**
 * El feed del Inicio: lo que el proyecto publica, con un pilar puesto o sin ninguno.
 *
 * `filter` nulo es «Todos», que es exactamente lo que el sitio entiende por no mandar el parámetro.
 * No hay un `PillarType.ALL` inventado: el filtro que no filtra es la ausencia de filtro.
 *
 * **Ya no sabe qué es un término de búsqueda.** Antes decidía por él a qué endpoint preguntar, y era
 * la misma clase la que servía al feed y al buscador. Ahora buscar es otra pantalla con su propio
 * ViewModel, y lo único que las dos comparten —acumular páginas sin pedir dos veces lo mismo— vive
 * en [PublicationPager].
 *
 * **Acumula aquí y no en la pantalla** porque sobrevivir a la navegación es justamente lo que se
 * necesita: bajar veinte publicaciones, entrar a un pilar y volver no puede costar volver a
 * bajarlas.
 */
class HomeFeedViewModel(
    private val getCatalog: GetCatalogUseCase,
    pageSize: Int = CatalogRepository.DEFAULT_PAGE_SIZE,
) : ViewModel() {

    private val pager = PublicationPager(viewModelScope, pageSize)

    val state: StateFlow<PublicationListUiState> = pager.state

    init {
        load(filter = null)
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
        if (pillar == pager.state.value.filter) return
        load(pillar)
    }

    /** Pide la página siguiente. La pantalla lo llama al pintar la última fila. */
    fun loadMore(): Unit = pager.loadMore()

    /** Reintenta lo que falló, sin tocar lo que ya se había leído. */
    fun retry(): Unit = pager.retry()

    private fun load(filter: PillarType?) {
        pager.restart(filter = filter) { page, size ->
            getCatalog(pillar = filter, page = page, pageSize = size)
        }
    }
}
