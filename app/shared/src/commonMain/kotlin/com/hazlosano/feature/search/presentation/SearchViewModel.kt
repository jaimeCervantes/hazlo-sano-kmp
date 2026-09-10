package com.hazlosano.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazlosano.core.presentation.PublicationListUiState
import com.hazlosano.core.presentation.PublicationPager
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.repository.CatalogRepository
import com.hazlosano.domain.usecase.SearchCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lo que se busca desde la barra de arriba, y lo que el buscador del sitio contesta.
 *
 * **El término y los resultados van por separado y no en un solo estado.** No es una omisión: son
 * dos cosas con ritmos distintos, y lo escrito tiene que aparecer en el campo **en el acto**,
 * mientras que la respuesta tarda lo que tarde la red. Juntarlos en un flujo combinado habría hecho
 * que la letra recién tecleada esperase a una recomposición para verse.
 *
 * De ahí sale también cómo se abre y se cierra la pantalla de resultados: está abierta exactamente
 * cuando [query] no está en blanco. No hay una segunda bandera que pudiera discrepar del término.
 */
class SearchViewModel(
    private val searchCatalog: SearchCatalogUseCase,
    pageSize: Int = CatalogRepository.DEFAULT_PAGE_SIZE,
    /**
     * Cuánto se espera desde la última tecla antes de preguntarle al sitio.
     *
     * Sin esta espera, escribir «miel» son cuatro búsquedas de las que tres sobran, y llegan
     * desordenadas. Entra por constructor para que un test no tenga que esperar de verdad.
     */
    private val typingPause: Duration = TYPING_PAUSE,
) : ViewModel() {

    private val pager = PublicationPager(viewModelScope, pageSize)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<PublicationListUiState> = pager.state

    /**
     * A qué pilar se están acotando los resultados, o a ninguno.
     *
     * Vive aquí y no se lee de la lista porque **sobrevive a que no haya lista**: al cerrar la
     * búsqueda no queda ningún estado del que sacarlo, y hay que poder decir que vuelve a «Todos».
     */
    private var filter: PillarType? = null

    /**
     * Lo que se escribe, que no se pregunta en el acto.
     *
     * El término se guarda ya —para que lo escrito aparezca sin retraso y la pantalla de resultados
     * se abra con la primera letra— pero la petición espera a que se deje de teclear.
     */
    fun type(text: String) {
        if (text == _query.value) return
        if (text.isBlank()) {
            // Borrar lo escrito es cerrar la búsqueda, y no buscar la cadena vacía.
            clear()
            return
        }
        _query.value = text
        ask(wait = typingPause)
    }

    /**
     * Elegir un pilar **acota** la búsqueda en curso, no la cancela.
     *
     * Es lo que hace el sitio con su parámetro `pillar`: buscar «miel» en Nutrición es una pregunta,
     * y perder el término al tocar un chip obligaría a escribirlo otra vez.
     */
    fun select(pillar: PillarType?) {
        if (pillar == filter) return
        filter = pillar
        if (_query.value.isBlank()) return
        ask(wait = Duration.ZERO)
    }

    /**
     * Cierra la búsqueda: olvida el término, el pilar y lo encontrado.
     *
     * Es lo que hacen las tres formas de volver —la flecha, el gesto del sistema y borrar lo
     * escrito— porque son la misma: guardar el término dejaría la barra enseñando una pregunta cuya
     * respuesta ya no se está mirando.
     */
    fun clear() {
        _query.value = ""
        filter = null
        pager.clear()
    }

    /** Pide la página siguiente de resultados. La pantalla lo llama al pintar la última fila. */
    fun loadMore(): Unit = pager.loadMore()

    /** Reintenta lo que falló, sin tocar lo que ya se había leído. */
    fun retry(): Unit = pager.retry()

    private fun ask(wait: Duration) {
        val term = _query.value
        val pillar = filter
        pager.restart(filter = pillar, wait = wait) { page, size ->
            searchCatalog(query = term, pillar = pillar, page = page, pageSize = size)
        }
    }

    private companion object {
        /**
         * Bastante para que una palabra escrita del tirón sea una sola búsqueda, y poco para que no
         * se note al parar.
         */
        val TYPING_PAUSE: Duration = 350.milliseconds
    }
}
