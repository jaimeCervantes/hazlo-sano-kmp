package com.hazlosano.data.catalog

import com.hazlosano.domain.model.PillarType
import com.hazlosano.domain.model.VisitorLocation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * El cliente del catálogo del sitio.
 *
 * La URL base entra por constructor en lugar de estar escrita dentro de una función: es lo único
 * de aquí que cambia entre entornos, y enterrarla haría falta recompilar para apuntar a otro sitio.
 */
internal class CatalogApi(
    private val client: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    suspend fun fetchPillarPage(
        pillar: PillarType,
        page: Int,
        pageSize: Int,
        location: VisitorLocation?,
    ): CatalogResponseDto = client.get("$baseUrl/api/posts/page/$page/pageSize/$pageSize") {
        parameter("locale", LOCALE)
        parameter("pillar", pillar.key)
        location?.asCookieValue()?.let { header(HttpHeaders.Cookie, "$LOCATION_COOKIE=$it") }
    }.body()

    /**
     * La ubicación viaja como la cookie que el sitio ya sabe leer: `lat,lng,ts`.
     *
     * No es un rodeo — el sitio la parsea de una cabecera y **valida lo que llegue**, con su propio
     * comentario diciendo que «una cookie la escribe cualquiera». Mandarla desde el app es el mismo
     * nivel de confianza que mandarla desde un navegador.
     *
     * El precio es acoplarse al nombre y al formato de algo interno del web. El fallo, si eso
     * cambia, es **suave**: `parseFix` devuelve nulo, el listado sale ordenado por fecha y no se
     * rompe nada. El remedio anotado es un endpoint propio con `lat`/`lng` explícitos.
     */
    private fun VisitorLocation.asCookieValue(): String? {
        if (!isValid) return null
        val stamp = fixedAtEpochMillis?.let { ",$it" }.orEmpty()
        return "$latitude,$longitude$stamp"
    }

    companion object {
        const val DEFAULT_BASE_URL: String = "https://hazlosano.com"

        /** El nombre que espera `readVisitorLocation` en el sitio. */
        private const val LOCATION_COOKIE = "hs_location"

        /** El app se publica solo en español; el sitio cae a su idioma por omisión si no llega. */
        private const val LOCALE = "es"

        /**
         * Tolerante a propósito: el sitio añade campos al mapper sin avisar a nadie, y una clave
         * nueva no puede dejar el catálogo en blanco.
         */
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
