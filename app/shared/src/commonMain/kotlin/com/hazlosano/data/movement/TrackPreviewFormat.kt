package com.hazlosano.data.movement

import com.hazlosano.domain.feature.movement.model.UserLocation

/**
 * Cómo viaja a la base la silueta de una salida.
 *
 * Es un formato de almacenamiento y no un modelo de dominio, así que vive en la capa de datos: nadie
 * fuera de aquí tiene que saber que la silueta se guarda como texto.
 *
 * **Por qué se guarda y no se deriva.** El pilar tiene una regla —las cifras se calculan de los
 * puntos cada vez— y esto parece contradecirla. Es el mismo caso que `distanceTraveled`, que ya es la
 * excepción declarada y por el mismo motivo: **la lista no puede leer todos los puntos de todas las
 * salidas** para dibujarse. Y una silueta no es una cifra: congelarla no congela ninguna medición,
 * porque no afirma nada medible.
 *
 * El formato es deliberadamente simple —pares separados por `;`, coordenada por `,`— en vez de una
 * polilínea codificada. Con 200 puntos son unos 3 KB por salida, que para una tabla que ya guarda
 * cada lectura del GPS no es nada, y a cambio la columna se puede leer a ojo cuando algo va mal.
 */
internal object TrackPreviewFormat {

    private const val POINT_SEPARATOR = ';'
    private const val COORDINATE_SEPARATOR = ','

    fun encode(points: List<UserLocation>): String? {
        if (points.isEmpty()) return null
        return points.joinToString(POINT_SEPARATOR.toString()) { point ->
            "${point.latitude}$COORDINATE_SEPARATOR${point.longitude}"
        }
    }

    /**
     * Lo que se pueda leer, y nada más.
     *
     * Un par ilegible se salta en vez de tumbar la lista entera: la silueta es decoración, y una
     * salida que no se puede dibujar sigue siendo una salida que se puede abrir. Una columna a medio
     * escribir no puede costar el historial.
     */
    fun decode(stored: String?): List<UserLocation> {
        if (stored.isNullOrBlank()) return emptyList()
        return stored.split(POINT_SEPARATOR).mapNotNull { pair ->
            val parts = pair.split(COORDINATE_SEPARATOR)
            if (parts.size != 2) return@mapNotNull null
            val latitude = parts[0].toDoubleOrNull() ?: return@mapNotNull null
            val longitude = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            UserLocation(latitude = latitude, longitude = longitude, timestamp = 0)
        }
    }
}
