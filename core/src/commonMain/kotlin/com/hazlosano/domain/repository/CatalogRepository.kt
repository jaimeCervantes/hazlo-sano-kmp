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
     * Una página del catálogo de un pilar.
     *
     * [location] es opcional porque no todos los targets pueden saberla; cuando viaja, el sitio
     * ordena por cercanía en vez de por fecha.
     */
    suspend fun getPillarCatalog(
        pillar: PillarType,
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
