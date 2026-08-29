package com.hazlosano.data.product

import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.HazloSeller
import com.hazlosano.domain.model.PillarType

/**
 * El almacenamiento local de las publicaciones.
 *
 * Desde que el catálogo se lee del sitio, esta tabla es **caché y no origen**: lo que hay aquí es
 * lo último que se pudo leer, y existe para que el app abra sin red.
 */
interface ProductDataSource {
    suspend fun searchProducts(query: String, limit: Int = 50, offset: Int = 0): List<HazloProduct>
    suspend fun getByCategory(category: String, limit: Int = 50, offset: Int = 0): List<HazloProduct>
    suspend fun getAll(limit: Int = 50, offset: Int = 0): List<HazloProduct>
    suspend fun getById(id: String): HazloProduct?
    suspend fun saveProducts(products: List<HazloProduct>)
    suspend fun saveSellers(sellers: List<HazloSeller>)
    suspend fun count(): Long
    suspend fun deleteAll()

    /** Lo último que se leyó de un pilar, en el orden en que el sitio lo mandó. */
    suspend fun getByPillar(pillar: PillarType, limit: Int = 50, offset: Int = 0): List<HazloProduct>

    /**
     * Cuántas publicaciones hay guardadas de un pilar.
     *
     * Es lo que distingue «este pilar no se ha leído nunca» de «este pilar está vacío en el sitio»,
     * que son los dos finales que el usuario tiene que poder diferenciar.
     */
    suspend fun countByPillar(pillar: PillarType): Long

    /**
     * Sustituye la caché de un pilar por lo recién leído, en una sola transacción.
     *
     * Borra y luego inserta: sin el borrado, una publicación retirada del sitio se quedaría en el
     * dispositivo para siempre, porque `INSERT OR REPLACE` solo refresca lo que sigue llegando.
     */
    suspend fun replacePillar(pillar: PillarType, publications: List<HazloProduct>)
}

expect fun createProductDataSource(): ProductDataSource
