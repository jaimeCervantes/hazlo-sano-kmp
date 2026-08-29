package com.hazlosano.domain.model

/**
 * Desde dónde mira quien abre el catálogo.
 *
 * El sitio ordena por cercanía cuando la recibe y por fecha cuando no, así que **`null` es una
 * respuesta de primera clase**: un escritorio sin GPS no es un error, es un visitante sin ubicación.
 *
 * [fixedAtEpochMillis] viaja porque el sitio compara la frescura de esta posición contra la que el
 * bot de WhatsApp guardó en la cuenta, y se queda con la más reciente. Sin fecha, la suya gana.
 */
data class VisitorLocation(
    val latitude: Double,
    val longitude: Double,
    val fixedAtEpochMillis: Long? = null,
) {
    /** Descarta lo que no puede ser una coordenada antes de mandarlo, igual que hace el sitio. */
    val isValid: Boolean
        get() = latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
}
