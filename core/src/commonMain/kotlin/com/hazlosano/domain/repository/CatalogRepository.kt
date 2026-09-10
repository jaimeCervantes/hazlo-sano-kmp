package com.hazlosano.domain.repository

import com.hazlosano.domain.model.CatalogPage
import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation

/**
 * De dónde sale el catálogo que se enseña.
 *
 * Va aparte de `ProductRepository` a propósito, por la misma segregación que separó rutas de
 * sesiones: aquel busca en lo que hay guardado en el dispositivo; este trae páginas del sitio y cae
 * a lo guardado solo cuando no puede. Son dos ciclos de vida distintos sobre la misma tabla, y
 * mezclarlos obligaría a cada implementación a deber métodos que no le tocan.
 */
interface CatalogRepository {

    /**
     * Una página del catálogo, de un pilar o de todos.
     *
     * **[pillar] es opcional porque en el sitio lo es**: su endpoint acepta el filtro como un
     * parámetro que puede no ir, y sin él devuelve las publicaciones de los cuatro pilares. Tener
     * aquí dos operaciones —una con pilar y otra sin él— seria partir en dos lo que al otro lado es
     * una sola, y dejar a cada implementación debiendo la mitad que no le toca.
     *
     * [location] es opcional porque no todos los targets pueden saberla; cuando viaja, el sitio
     * ordena por cercanía en vez de por fecha.
     */
    suspend fun getCatalog(
        pillar: PillarType? = null,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        location: VisitorLocation? = null,
    ): CatalogPage

    /**
     * Buscar publicaciones por texto, opcionalmente dentro de un pilar.
     *
     * Va aparte de [getCatalog] y no como un parametro suyo porque **al otro lado son dos
     * endpoints con dos contratos**: uno pagina con `nextPage` y el otro con un total, y uno
     * entrega el idioma aplanado y el otro no. Esconder eso tras un parametro opcional habria
     * hecho que una de las dos mitades mintiera sobre lo que sabe.
     *
     * Devuelve el mismo [CatalogPage] porque lo que la pantalla necesita saber es lo mismo: si
     * llego del sitio, si vino de lo guardado, y si quedan mas.
     */
    suspend fun searchCatalog(
        query: String,
        pillar: PillarType? = null,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        location: VisitorLocation? = null,
    ): CatalogPage

    companion object {
        /**
         * Veinte y no los cuatro que trae el sitio por omisión.
         *
         * Su número está pensado para el scroll infinito de una página web, donde pedir más es
         * gratis. En un teléfono, cada petición cuesta una espera con la pantalla en blanco.
         */
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}
